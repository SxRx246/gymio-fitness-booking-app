# Gymio deployment (CI/CD)

The `Jenkinsfile` at the repository root implements the full pipeline:

```
GitHub commit
  -> poll SCM every 2 min
  -> ./mvnw clean verify  (five Maven modules, JUnit tests)
  -> docker build         (one image per service)
  -> push to Amazon ECR   (tagged with the Git commit SHA)
  -> render Dockerrun.aws.json + .ebextensions
  -> upload bundle to S3, create EB application version, update environment
  -> wait for the new version to be live
  -> GET /actuator/health through the EB load balancer
  -> roll back to the previous version if anything fails
```

AWS calls use the **IAM role attached to the Jenkins agent**. No AWS access keys
exist in the repository, in Jenkins, or in this pipeline.

---

## 1. One-time setup

### 1.1 Jenkins agent

The agent needs:

| Tool | Why |
|---|---|
| JDK 17 | Maven build |
| Docker | image build + push |
| AWS CLI v2 | ECR, S3, Elastic Beanstalk |
| `zip` | source bundle for Elastic Beanstalk |
| `curl` | deployment health check |

`JAVA_HOME_DIR` in the `environment` block defaults to `/opt/java/openjdk`;
change it if your agent uses a different path.

### 1.2 IAM permissions for the Jenkins agent

Attach this policy to the Jenkins agent's instance profile (EC2) or task role
(ECS). Replace `<region>`, `<account-id>` and the bucket name to match.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PushImagesToEcr",
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:BatchGetImage",
        "ecr:CompleteLayerUpload",
        "ecr:DescribeRepositories",
        "ecr:InitiateLayerUpload",
        "ecr:PutImage",
        "ecr:UploadLayerPart"
      ],
      "Resource": "*"
    },
    {
      "Sid": "DeployToBeanstalk",
      "Effect": "Allow",
      "Action": [
        "elasticbeanstalk:CreateApplicationVersion",
        "elasticbeanstalk:DeleteApplicationVersion",
        "elasticbeanstalk:DescribeApplications",
        "elasticbeanstalk:DescribeApplicationVersions",
        "elasticbeanstalk:DescribeEnvironments",
        "elasticbeanstalk:UpdateEnvironment"
      ],
      "Resource": "*"
    },
    {
      "Sid": "UploadSourceBundle",
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:GetObject"],
      "Resource": [
        "arn:aws:s3:::elasticbeanstalk-<region>-<account-id>",
        "arn:aws:s3:::elasticbeanstalk-<region>-<account-id>/*"
      ]
    }
  ]
}
```

`ecr:GetAuthorizationToken` must stay on `"Resource": "*"` — it is an
account-level operation.

### 1.3 ECR repositories

The pipeline expects repositories named `gymio/<service>`:

```
gymio/api-gateway
gymio/user-service
gymio/fitness-booking-service
gymio/membership-payment-service
gymio/notification-service
```

If yours are named differently, edit the right-hand side of `SERVICE_LIST` in
`Jenkinsfile`:

```groovy
def SERVICE_LIST = [
    'api-gateway=gymio/api-gateway',
    ...
]
```

### 1.4 Elastic Beanstalk application and environment

The pipeline **updates** an existing environment; it does not create one.
Create the application and a **multi-container Docker** environment once.

Using the EB CLI (easiest):

```bash
eb init   # region, application "gymio", platform "Docker" (multi-container)
eb create gymio-env
```

Or with the AWS CLI:

```bash
aws elasticbeanstalk create-application --application-name gymio

# Pick the current multi-container Docker stack name:
aws elasticbeanstalk list-available-solution-stacks \
  --query "SolutionStacks[?contains(@, 'Docker')]"

aws elasticbeanstalk create-environment \
  --application-name gymio \
  --environment-name gymio-env \
  --solution-stack-name "<stack name from the list above>" \
  --option-settings \
    Namespace=aws:elasticbeanstalk:environment,OptionName=EnvironmentType,Value=LoadBalanced \
    Namespace=aws:autoscaling:launchconfiguration,OptionName=InstanceType,Value=t3.medium \
    Namespace=aws:autoscaling:launchconfiguration,OptionName=IamInstanceProfile,Value=aws-elasticbeanstalk-ec2-role \
    Namespace=aws:elasticbeanstalk:environment,OptionName=ServiceRole,Value=aws-elasticbeanstalk-service-role
```

Requirements:

- **Same region** as your ECR repositories and the S3 bucket.
- **Same VPC** as the existing PostgreSQL (`172.31.39.199`) and Kafka
  (`172.31.39.10`) hosts — the EB instances reach them over private IPs.
- The **RDS security group** must allow inbound 5432 from the EB instance
  security group (or the VPC CIDR), and Kafka 9092 likewise.
- Instance type **t3.medium or larger**: five JVMs share one instance
  (4 x 512 MB + 1 x 256 MB of container memory, plus the ECS agent).

The S3 bucket defaults to `elasticbeanstalk-<region>-<account-id>`, which
Elastic Beanstalk creates automatically on first use. Override `EB_S3_BUCKET`
in the `environment` block if you keep bundles elsewhere.

### 1.5 Jenkins job

Create a Pipeline job (or a Multibranch Pipeline job) with:

- **Git SCM**: `https://github.com/SxRx246/gymio-fitness-booking-app.git`,
  branch `*/main`.
- Leave the mode as *Pipeline script from SCM* so the `Jenkinsfile` is the
  single source of truth.
- No AWS credentials to configure — the agent's IAM role is used.
- The `pollSCM` trigger is declared in the `Jenkinsfile`; no extra Jenkins
  configuration is required.

### 1.6 Jenkins credentials

Create these under **Manage Jenkins -> Credentials** with the exact IDs below.
The pipeline fails fast if one is missing.

| ID | Type | Used by |
|---|---|---|
| `gymio-db` | Username/Password | Maven tests and EB containers |
| `gymio-jwt-secret` | Secret text | JWT signing |
| `gymio-internal-auth-secret` | Secret text | gateway <-> service trust |
| `gymio-jwt-expiration` | Secret text | `JWT_EXPIRATION_MS`, e.g. `86400000` |
| `gymio-mail` | Username/Password | notification service (Gmail) |

`PUBLIC_BASE_URL` (used in verification/reset e-mails) defaults to
`http://<EB environment CNAME>`. Override it in the `environment` block if you
have a custom domain.

---

## 2. How a deployment works

| Stage | What happens |
|---|---|
| `Initialize` | Resolves the AWS account, ECR registry, EB CNAME and previous version label. |
| `Build & Test` | `./mvnw clean verify` against `gymio_test`, with credentials injected. |
| `Build Docker Images` | One `docker build` per service, tagged `<registry>/gymio/<svc>:<sha>`. |
| `Push to Amazon ECR` | Pushes the SHA tag and `latest`. |
| `Deploy to Elastic Beanstalk` | Renders `Dockerrun.aws.json`, uploads the bundle to S3, creates the EB application version, updates the environment. |
| `Verify Deployment` | Waits for the new version label to be `Ready`, then polls `/actuator/health` for up to ~3 minutes. |

Only `main` is deployed; other branches are built, tested and pushed to ECR but
not deployed.

### Container topology

`Dockerrun.aws.json` defines five containers in one ECS task:

| Container | Host port | Memory |
|---|---|---|
| `api-gateway` | 80 -> 8080 | 512 MB |
| `user-service` | – | 512 MB |
| `fitness-booking-service` | – | 512 MB |
| `membership-payment-service` | – | 512 MB |
| `notification-service` | – | 256 MB |

The Elastic Beanstalk multi-container platform runs the task in **bridge**
network mode and ignores `networkMode` in `Dockerrun.aws.json`, so containers
find each other through Docker `links` and by container name:

- gateway -> `http://user-service:8081`, `http://fitness-booking-service:8082`, ...
- fitness-booking-service -> `http://user-service:8081`

`localhost` would resolve to the container itself, not to a peer, so it is not
used anywhere in the container definitions.

Only the gateway publishes a port; the load balancer forwards to instance port
80, which maps to the gateway's 8080.

### Health check

`GET http://<eb-cname>/actuator/health` must return `{"status":"UP"}`.
The gateway's `GatewayAuthFilter` lists `/actuator/health` as a public
endpoint, and `.ebextensions` points the EB health check at the same path.

---

## 3. Rollback

The previous EB application version label is captured before each update.
If the health check fails and `ROLLBACK_ON_FAILURE` is enabled (the default),
the environment is immediately put back on that version.

Manual rollback:

```bash
aws elasticbeanstalk describe-application-versions --application-name gymio \
  --query 'ApplicationVersions[].[VersionLabel,Status]' --output table

aws elasticbeanstalk update-environment \
  --application-name gymio \
  --environment-name gymio-env \
  --version-label "<previous version>"
```

---

## 4. Security notes

- Secrets live in the Jenkins credentials store and are injected only for the
  duration of a stage. Jenkins masks them in the console output.
- No AWS access keys anywhere: authentication is the agent's IAM role.
- The rendered `Dockerrun.aws.json` embeds the container environment, so the
  source bundle uploaded to S3 contains the secrets in plain text. The EB
  bucket is SSE-S3 encrypted by default, but restrict `s3:GetObject` on it and
  add a lifecycle rule to expire old `gymio/*.zip` objects.
- To remove secrets from the bundle entirely, store them in AWS Secrets Manager
  or SSM Parameter Store (SecureString), grant the EB instance profile read
  access, and fetch them at container start instead of rendering them into the
  task definition. That requires an entrypoint change in the Dockerfiles and
  is out of scope for this pipeline.

---

## 5. Known limitations

- **Single instance.** Rate limiting and SSE connection tracking are in-memory,
  so running more than one instance would split both. Scale out only after
  moving them to Redis.
- **Single-container health check only.** EB reports on the task as a whole and
  the pipeline checks the gateway. The other four services are only reachable
  inside the task, so they are not probed individually.
- **Database schema is managed by Hibernate** (`ddl-auto=update`). There are no
  migrations; review schema changes before deploying.
- **No zero-downtime guarantee.** A single-instance rolling deployment causes a
  short interruption while the instance is replaced.
- **Docker legacy `links`.** EB's bridge networking relies on Docker links, an
  older mechanism. A durable alternative is the AL2023 Docker platform with
  `docker-compose.yml`, where services get DNS names on a user-defined network.
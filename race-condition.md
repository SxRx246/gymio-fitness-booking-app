# Race Condition in Gymio

## 1. Shared Resource Being Accessed

In Gymio, the shared resource is the available capacity of a fitness class. Each class has a maximum number of bookings, and multiple customers may attempt to book the same class.

For example, a class has a maximum capacity of 10 customers. The system must ensure that the number of confirmed bookings never exceeds this limit.

## 2. Why Multiple Operations Can Occur at the Same Time

Gymio is a web application that allows multiple customers to send booking requests simultaneously. Each request is processed by the backend, and different requests may execute concurrently.

For example, two customers may try to book the last available place in the same fitness class at almost the same time.

## 3. The Race Condition That Could Occur

A race condition could occur if two booking requests check the number of confirmed bookings before either request saves its new booking.

Suppose a class has a capacity of 10 and already has 9 confirmed bookings:

1. Customer A requests a booking and the system finds 9 confirmed bookings.
2. Customer B requests a booking before Customer A's request finishes and also finds 9 confirmed bookings.
3. Both requests conclude that one place is available.
4. Both requests attempt to create a confirmed booking.

Because both requests used the same outdated booking count, they may both attempt to reserve the final place.

## 4. Incorrect Result Without Concurrency Control

Without concurrency control, both requests could succeed, resulting in 11 confirmed bookings for a class with a capacity of 10.

This would violate Gymio's business rules, allow the class to become overbooked, and create inconsistent booking data.

## 5. How Gymio Prevents the Problem

Gymio prevents this race condition by using a database transaction and a pessimistic write lock when retrieving the fitness class during booking creation.

The booking process uses:

`fitnessClassRepository.findByIdForUpdate(fitnessClassId)`

The repository method applies a pessimistic write lock to the selected fitness class. When one transaction obtains the lock, another transaction attempting to lock the same class must wait until the first transaction completes.

The booking process can then check the latest booking count and determine whether capacity is still available before confirming the booking. Once the first transaction commits, the waiting transaction can continue and check the updated state.

For example:

1. Customer A obtains the lock and checks the class capacity.
2. Customer A confirms the booking, bringing the confirmed booking count to 10, and the transaction commits.
3. Customer B obtains the lock and checks the updated booking count.
4. Customer B's request is rejected because the class has reached its maximum capacity.

The transaction ensures that the booking operation is handled as a unit, while the pessimistic write lock prevents concurrent booking requests from making conflicting decisions about the same class.

## Conclusion

The main concurrency risk in Gymio is that simultaneous booking requests could reserve more places than a fitness class can accommodate. Using a database transaction and a pessimistic write lock helps ensure that each booking request checks the latest class state before confirming a booking, protecting the class capacity limit.

The concurrency integration test should also be run against the configured test database to verify that simultaneous requests cannot cause the confirmed booking count to exceed the class capacity.
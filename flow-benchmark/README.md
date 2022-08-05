## Benchmarking

Start docker image services
Start application server in development mode
Submit job through app with desired query to monitor (TODO - automate this)
with environmentId set to emily.

`mvn install`

## Building

`mvn clean verify`

## Running

`java -jar target/benchmarks.jar`

469
361
417
430
436
428
441
430
441
430
449
430
436
420
442
434

444
433
443
446
442 Last 5 = 441.6 miliseconds.

586
313
431
422
438
408
426
335
548
338
438
440
427
445
398
436

425
439
450
440
444

459
254
426
438
436
437
431
441
443
442
439
438
438
445
433
447

448
429
439
444
436

460
525
388
428
438
421
401
437
412
440
443
435
446
457
444
428

447
441
443
441
449

505
123
357
425
431
439
434
438
446
438
433
448
440
440
440
447

421
435
446
438
442

These above numbers are from a single update, and a single job running on the cluter.
I'm connecting with the default kafka super user - not over SSL.

How does it compare when I run 5 more connectors on the same database but a different job on the same cluster, which will also receive results from the job since it's monitoring the same table.

With two jobs running: separate databases (No concurrent changes though):;
455
557
437
435
435
439
431
455
445
448
441
446
446
448
444
439

419
439
447
454
444 = 440.6

516
181
436
430
437
435
439
433
427
433
450
433
427
452
436
440

444
444
459
435
453 = 447

560
424
392
434
446
429
432
437
443
446
448
445
404
449
426
404

440
434
440
443
424 = 436.2

563
416
431
432
434
436
445
445
442
440
444
448
442
446
438
447

445
439
409
338
446 = 415.4

620
139
431
421
435
444
415
435
459
444
443
445
457
445
452
441

454
431
448
453
445 = 446.2

38
78-38 = 40
117 - 79 = 40
160 - 118 = 40

198- 169 = 30
234-194 = 40

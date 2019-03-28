/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.example.panache;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.junit.jupiter.api.Assertions;

import io.reactivex.Single;

/**
 * Various tests covering Panache functionality. All tests should work in both standard JVM and SubstrateVM.
 */
@Path("test")
public class TestEndpoint {

    @GET
    @Path("rx-model")
    public Single<String> testRxModel() {
        return RxPerson.findAll().toList()
                .flatMap(persons -> {
                    Assertions.assertEquals(0, persons.size());

                    return makeSavedRxPerson();
                }).flatMap(person -> {
                    Assertions.assertNotNull(person.id);

                    return RxPerson.findAll().toList()
                            .flatMapMaybe(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return RxPerson.findById(person.id);
                            }).flatMapSingle(byId -> {
                                Assertions.assertEquals(person, byId);

                                return RxPerson.find("name = ?1", "stef").toList();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return RxPerson.find("name", "emmanuel").toList();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(0, persons.size());

                                return RxPerson.count();
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return RxPerson.count("name = ?1", "stef");
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return RxPerson.count("name", "stef");
                            }).flatMapCompletable(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return person.delete();
                            })
                            .andThen(Single.defer(() -> RxPerson.count()))
                            .flatMap(count -> {
                                Assertions.assertEquals(0, (long) count);

                                return makeSavedRxPerson();
                            })
                            .flatMap(p -> RxPerson.count())
                            .flatMap(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return RxPerson.deleteAll();
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return RxPerson.count();
                            }).flatMap(count -> {
                                Assertions.assertEquals(0, (long) count);

                                return makeSavedRxPerson();
                            }).flatMap(p -> RxPerson.delete("name = ?1", "emmanuel"))
                            .flatMap(count -> {
                                Assertions.assertEquals(0, (long) count);

                                return RxPerson.delete("name", "stef");
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return makeSavedRxPerson();
                            }).flatMap(p -> {

                                RxDog dog = new RxDog("octave", "dalmatian");
                                dog.owner = Single.just(p);
                                return dog.save();
                            }).flatMapMaybe(d -> {
                                // get it from the DB
                                return RxDog.<RxDog> findById(d.id);
                                // check the lazy single
                            }).flatMapSingle(d -> d.owner)
                            // check the lazy list
                            .flatMap(p -> p.dogs.toList())
                            .flatMap(dogs -> {
                                Assertions.assertEquals(1, dogs.size());

                                // cleanup
                                return RxPerson.deleteAll();
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count.longValue());
                                return RxDog.deleteAll();
                            }).map(count -> {
                                Assertions.assertEquals(1, count.longValue());
                                return "OK";
                            });
                });
    }

    private Single<? extends RxPerson> makeSavedRxPerson() {
        RxPerson person = new RxPerson();
        person.name = "stef";
        person.status = Status.LIVING;
        //        person.address = new SequencedAddress("stef street");
        //        person.address.save();
        try {
            return person.save();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Inject
    RxPersonRepository rxPersonRepository;
    @Inject
    RxDogDao rxDogRepository;

    @GET
    @Path("rx-model-dao")
    public Single<String> testRxModelDao() {
        System.err.println("A: CL: "+Thread.currentThread().getContextClassLoader());
        return rxPersonRepository.findAll().toList()
                .flatMap(persons -> {
                    Assertions.assertEquals(0, persons.size());

                    System.err.println("B: CL: "+Thread.currentThread().getContextClassLoader());
                    return makeSavedRxPersonDao();
                }).flatMap(person -> {
                    Assertions.assertNotNull(person.id);
                    System.err.println("C");

                    return rxPersonRepository.findAll().toList()
                            .flatMapMaybe(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                System.err.println("D");
                                return rxPersonRepository.findById(person.id);
                            }).flatMapSingle(byId -> {
                                Assertions.assertEquals(person, byId);

                                System.err.println("E");
                                return rxPersonRepository.find("name = ?1", "stef").toList();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                System.err.println("F");
                                return RxPerson.find("name", "emmanuel").toList();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(0, persons.size());

                                System.err.println("G");
                                return rxPersonRepository.count();
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, (long) count);

                                System.err.println("H");
                                return rxPersonRepository.count("name = ?1", "stef");
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, (long) count);

                                System.err.println("I");
                                return rxPersonRepository.count("name", "stef");
                            }).flatMapCompletable(count -> {
                                Assertions.assertEquals(1, (long) count);

                                System.err.println("J");
                                return rxPersonRepository.delete(person);
                            })
                            .andThen(Single.defer(() -> rxPersonRepository.count()))
                            .flatMap(count -> {
                                Assertions.assertEquals(0, (long) count);

                                System.err.println("K");
                                return makeSavedRxPersonDao();
                            })
                            .flatMap(p -> rxPersonRepository.count())
                            .flatMap(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return rxPersonRepository.deleteAll();
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return rxPersonRepository.count();
                            }).flatMap(count -> {
                                Assertions.assertEquals(0, (long) count);

                                return makeSavedRxPersonDao();
                            }).flatMap(p -> RxPerson.delete("name = ?1", "emmanuel"))
                            .flatMap(count -> {
                                Assertions.assertEquals(0, (long) count);

                                return rxPersonRepository.delete("name", "stef");
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return makeSavedRxPersonDao();
                            }).flatMap(p -> {
                                System.err.println("L");

                                RxDog dog = new RxDog("octave", "dalmatian");
                                dog.owner = Single.just(p);
                                return rxDogRepository.save(dog);
                            }).flatMapMaybe(d -> {
                                // get it from the DB
                                System.err.println("L2");
                                return rxDogRepository.findById(d.id);
                                // check the lazy single
                            }).flatMapSingle(d -> d.owner)
                            // check the lazy list
                            .flatMap(p -> p.dogs.toList())
                            .flatMap(dogs -> {
                                Assertions.assertEquals(1, dogs.size());

                                System.err.println("M");
                                // cleanup
                                return rxPersonRepository.deleteAll();
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count.longValue());
                                return rxDogRepository.deleteAll();
                            }).map(count -> {
                                Assertions.assertEquals(1, count.longValue());
                                return "OK";
                            });
                });
    }

    private Single<? extends RxPerson> makeSavedRxPersonDao() {
        RxPerson person = new RxPerson();
        person.name = "stef";
        person.status = Status.LIVING;
        //        person.address = new SequencedAddress("stef street");
        //        person.address.save();
        try {
            return rxPersonRepository.save(person);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }
}

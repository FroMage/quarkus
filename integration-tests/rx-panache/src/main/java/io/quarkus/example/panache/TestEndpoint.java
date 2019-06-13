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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.Assertions;
import org.reactivestreams.Publisher;

/**
 * Various tests covering Panache functionality. All tests should work in both standard JVM and SubstrateVM.
 */
@Path("test")
public class TestEndpoint {

    private <T> CompletionStage<List<T>> toList(Publisher<T> publisher) {
        return ReactiveStreams.fromPublisher(publisher).toList().run();
    }

    @GET
    @Path("rx-model")
    public CompletionStage<String> testRxModel() {

        return RxPerson.listAll()
                .thenCompose(persons -> {
                    Assertions.assertEquals(0, persons.size());

                    return makeSavedRxPerson();
                }).thenCompose(person -> {
                    Assertions.assertNotNull(person.id);

                    return RxPerson.listAll()
                            .thenCompose(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return RxPerson.findById(person.id);
                            }).thenCompose(byId -> {
                                Assertions.assertEquals(person, byId);

                                return RxPerson.list("name = ?1", "stef");
                            }).thenCompose(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return RxPerson.list("name", "emmanuel");
                            }).thenCompose(persons -> {
                                Assertions.assertEquals(0, persons.size());

                                return RxPerson.count();
                            }).thenCompose(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return RxPerson.count("name = ?1", "stef");
                            }).thenCompose(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return RxPerson.count("name", "stef");
                            }).thenCompose(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return person.delete();
                            })
                            .thenCompose(v -> RxPerson.count())
                            .thenCompose(count -> {
                                Assertions.assertEquals(0, (long) count);

                                return makeSavedRxPerson();
                            })
                            .thenCompose(p -> RxPerson.count())
                            .thenCompose(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return RxPerson.deleteAll();
                            }).thenCompose(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return RxPerson.count();
                            }).thenCompose(count -> {
                                Assertions.assertEquals(0, (long) count);

                                return makeSavedRxPerson();
                            }).thenCompose(p -> RxPerson.delete("name = ?1", "emmanuel"))
                            .thenCompose(count -> {
                                Assertions.assertEquals(0, (long) count);

                                return RxPerson.delete("name", "stef");
                            }).thenCompose(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return makeSavedRxPerson();
                            }).thenCompose(p -> {

                                RxDog dog = new RxDog("octave", "dalmatian");
                                dog.owner = CompletableFuture.completedFuture(p);
                                return dog.save();
                            }).thenCompose(d -> {
                                // get it from the DB
                                return RxDog.<RxDog> findById(d.id);
                                // check the lazy single
                            }).thenCompose(d -> d.owner)
                            // check the lazy list
                            .thenCompose(p -> toList(p.dogs))
                            .thenCompose(dogs -> {
                                Assertions.assertEquals(1, dogs.size());

                                // cleanup
                                return RxPerson.deleteAll();
                            }).thenCompose(count -> {
                                Assertions.assertEquals(1, count.longValue());
                                return RxDog.deleteAll();
                            }).thenApply(count -> {
                                Assertions.assertEquals(1, count.longValue());
                                return "OK";
                            });
                });
    }

    private CompletionStage<? extends RxPerson> makeSavedRxPerson() {
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
    public CompletionStage<String> testRxModelDao() {
        System.err.println("A: CL: " + Thread.currentThread().getContextClassLoader());
        return rxPersonRepository.listAll()
                .thenCompose(persons -> {
                    Assertions.assertEquals(0, persons.size());

                    System.err.println("B: CL: " + Thread.currentThread().getContextClassLoader());
                    return makeSavedRxPersonDao();
                }).thenCompose(person -> {
                    Assertions.assertNotNull(person.id);
                    System.err.println("C");

                    return rxPersonRepository.listAll()
                            .thenCompose(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                System.err.println("D");
                                return rxPersonRepository.findById(person.id);
                            }).thenCompose(byId -> {
                                Assertions.assertEquals(person, byId);

                                System.err.println("E");
                                return rxPersonRepository.list("name = ?1", "stef");
                            }).thenCompose(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                System.err.println("F");
                                return rxPersonRepository.list("name", "emmanuel");
                            }).thenCompose(persons -> {
                                Assertions.assertEquals(0, persons.size());

                                System.err.println("G");
                                return rxPersonRepository.count();
                            }).thenCompose(count -> {
                                Assertions.assertEquals(1, (long) count);

                                System.err.println("H");
                                return rxPersonRepository.count("name = ?1", "stef");
                            }).thenCompose(count -> {
                                Assertions.assertEquals(1, (long) count);

                                System.err.println("I");
                                return rxPersonRepository.count("name", "stef");
                            }).thenCompose(count -> {
                                Assertions.assertEquals(1, (long) count);

                                System.err.println("J");
                                return rxPersonRepository.delete(person);
                            })
                            .thenCompose(v -> rxPersonRepository.count())
                            .thenCompose(count -> {
                                Assertions.assertEquals(0, (long) count);

                                System.err.println("K");
                                return makeSavedRxPersonDao();
                            })
                            .thenCompose(p -> rxPersonRepository.count())
                            .thenCompose(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return rxPersonRepository.deleteAll();
                            }).thenCompose(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return rxPersonRepository.count();
                            }).thenCompose(count -> {
                                Assertions.assertEquals(0, (long) count);

                                return makeSavedRxPersonDao();
                            }).thenCompose(p -> RxPerson.delete("name = ?1", "emmanuel"))
                            .thenCompose(count -> {
                                Assertions.assertEquals(0, (long) count);

                                return rxPersonRepository.delete("name", "stef");
                            }).thenCompose(count -> {
                                Assertions.assertEquals(1, (long) count);

                                return makeSavedRxPersonDao();
                            }).thenCompose(p -> {
                                System.err.println("L");

                                RxDog dog = new RxDog("octave", "dalmatian");
                                dog.owner = CompletableFuture.completedFuture(p);
                                return rxDogRepository.save(dog);
                            }).thenCompose(d -> {
                                // get it from the DB
                                System.err.println("L2");
                                return rxDogRepository.findById(d.id);
                                // check the lazy single
                            }).thenCompose(d -> d.owner)
                            // check the lazy list
                            .thenCompose(p -> toList(p.dogs))
                            .thenCompose(dogs -> {
                                Assertions.assertEquals(1, dogs.size());

                                System.err.println("M");
                                // cleanup
                                return rxPersonRepository.deleteAll();
                            }).thenCompose(count -> {
                                Assertions.assertEquals(1, count.longValue());
                                return rxDogRepository.deleteAll();
                            }).thenApply(count -> {
                                Assertions.assertEquals(1, count.longValue());
                                return "OK";
                            });
                });
    }

    private CompletionStage<? extends RxPerson> makeSavedRxPersonDao() {
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

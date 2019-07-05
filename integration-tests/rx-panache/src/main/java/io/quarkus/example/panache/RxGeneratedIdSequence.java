package io.quarkus.example.panache;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import io.quarkus.panache.rx.PanacheRxEntityBase;

@Entity
public class RxGeneratedIdSequence extends PanacheRxEntityBase<RxGeneratedIdSequence> {

    @SequenceGenerator(name = "MY_SEQUENCE", sequenceName = "MY_SEQUENCE_NAME")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MY_SEQUENCE")
    @Id
    public long myId;
}

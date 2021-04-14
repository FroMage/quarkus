package io.quarkus.it.mongodb.panache.bugs;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;

//@MongoEntity(collection = "Bug16399")
public class Bug16399Entity extends PanacheMongoEntityBase {
    @BsonId
    @BsonProperty("_id")
    //    @Schema(example = "516448966")
    public Long id = -1L;

    @BsonProperty("last_activity")
    public LocalDateTime lastActivity = LocalDateTime.now();

    @BsonIgnore
    //TROUBLE METHOD CAUSES ERROR
    public Bug16399Entity setLastActivity(final LocalDateTime... lastActivities) {
        setLastActivity(Arrays.stream(lastActivities).max(LocalDateTime::compareTo).orElse(getTimeLastYear()));
        return this;
    }

    private LocalDateTime getTimeLastYear() {
        return null;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public Bug16399Entity setLastActivity(final LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
        return this;
    }

    public static List<Bug16399Entity> dbFindById(final Iterable<?> entities) {
        if (entities.iterator().hasNext()) {
            return find("_id in ?1", entities).list();
        }
        return Collections.emptyList();
    }

}

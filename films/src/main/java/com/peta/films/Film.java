package com.peta.films;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Builder
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "films")
public class Film {


    @EqualsAndHashCode.Exclude
    @Id
    private String id;

    @Field(type = FieldType.Text, name = "name")
    private String name;

    @EqualsAndHashCode.Exclude
    @Field(type = FieldType.Text, name = "path")
    private String path;

    @Field(type = FieldType.Long, name = "size")
    private Long size; // Size in bytes

    @EqualsAndHashCode.Exclude
    @Field(type = FieldType.Keyword, name = "eventType")
    private String eventType;

    @EqualsAndHashCode.Exclude
    @Field(type = FieldType.Date, name = "timestamp")
    private Instant timestamp; // Epoch millis

}

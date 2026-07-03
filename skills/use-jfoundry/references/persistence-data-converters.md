# Persistence Data Converters

Use this reference when a business project implements jfoundry aggregate repositories with `AggregateData`, `DataConverter`, MyBatis-Plus data objects, or MapStruct.

## Default Pattern

- Keep domain aggregates free of MyBatis, JPA, Spring, table annotations, type handlers, logical delete fields, and persistence data objects.
- Put table mapping, type handlers, fill strategies, nullable update strategies, and logical delete fields on the infrastructure data object.
- Let the data object extend `AggregateData<K>`, where `K` is a persistence-native ID type such as `String`, `Long`, or `UUID`.
- Keep the domain aggregate ID as a strong jMolecules `Identifier`.
- Convert the domain ID to the data ID in `toDataId(...)`.
- Prefer MapStruct for `toData(...)`.
- Keep `toEntity(...)` explicit and call the aggregate's `restore(...)` factory.
- Do not make converters Spring Beans by default. Prefer MapStruct's default component model and expose `INSTANCE = Mappers.getMapper(...)`.

## Converter Template

```java
@Mapper
public interface OrderDataConverter
        extends DataConverter<Order, OrderId, OrderData, String> {

    OrderDataConverter INSTANCE = Mappers.getMapper(OrderDataConverter.class);

    @Override
    @Mapping(target = "id", expression = "java(toDataId(entity.getId()))")
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deleterId", ignore = true)
    @Mapping(target = "deleterName", ignore = true)
    @Mapping(target = "deletedTime", ignore = true)
    OrderData toData(Order entity);

    @Override
    default Order toEntity(OrderData data) {
        if (data == null) {
            return null;
        }
        return Order.restore(
                OrderId.of(data.getId()),
                data.getStatus(),
                data.getAmount());
    }

    @Override
    default String toDataId(OrderId id) {
        return id == null ? null : id.value();
    }
}
```

Repository adapters should hold a static converter reference:

```java
private static final OrderDataConverter CONVERTER = OrderDataConverter.INSTANCE;

public OrderRepositoryImpl(OrderMapper mapper) {
    super(mapper, CONVERTER);
}
```

For Spring Boot applications, jfoundry auto-configuration injects `DomainEventContext` into `AbstractPersistenceRepository` internally. Business repository constructors should not expose `DomainEventContext`. For non-Spring or manual assembly, call `setDomainEventContext(...)` after constructing the repository.

## Audit Fields

Do not blindly ignore all audit fields.

- Ignore logical-delete fields and pure persistence fill fields when the domain aggregate does not own them.
- Map audit fields when the domain model intentionally carries an audit snapshot through `AuditableAggregateRoot` or `AuditableEntity`.
- Do not reintroduce persistence audit base classes into the domain model just to reduce converter code.

## Maven Notes

jfoundry BOMs manage MapStruct versions, but business projects still own compiler annotation processor configuration. Put MapStruct and Lombok processors in the module that compiles the converter, usually infrastructure.

Use `-Amapstruct.unmappedTargetPolicy=ERROR` when the project is ready to fail fast on unmapped data fields.

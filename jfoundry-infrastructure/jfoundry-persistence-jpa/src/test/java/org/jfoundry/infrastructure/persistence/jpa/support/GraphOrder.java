package org.jfoundry.infrastructure.persistence.jpa.support;

import org.jfoundry.domain.entity.agg.BaseAggregateRoot;

import java.util.List;

public final class GraphOrder extends BaseAggregateRoot<GraphOrder, GraphOrderId> {

    private List<String> lineSkus;

    private GraphOrder(GraphOrderId id, List<String> lineSkus) {
        super(id);
        this.lineSkus = List.copyOf(lineSkus);
    }

    public static GraphOrder create(GraphOrderId id, List<String> lineSkus) {
        return new GraphOrder(id, lineSkus);
    }

    public static GraphOrder restore(GraphOrderId id, List<String> lineSkus) {
        return new GraphOrder(id, lineSkus);
    }

    public void replaceLines(List<String> lineSkus) {
        this.lineSkus = List.copyOf(lineSkus);
    }

    public List<String> lineSkus() {
        return lineSkus;
    }
}

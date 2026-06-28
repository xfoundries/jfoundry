package org.jfoundry.test.archunit.fixture.hexagonalconventions.invalid.persistenceleak.application;

import org.jfoundry.architecture.hexagonal.Application;
import org.jfoundry.test.archunit.fixture.hexagonalconventions.invalid.persistenceleak.mybatis.mapper.HelpDocumentMapper;

@Application
public class LeakyApplicationService {

    private final HelpDocumentMapper mapper;

    public LeakyApplicationService(HelpDocumentMapper mapper) {
        this.mapper = mapper;
    }
}

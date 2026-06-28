package org.jfoundry.test.archunit.fixture.hexagonalconventions.valid.adapter.out;

import org.jfoundry.architecture.hexagonal.SecondaryAdapter;
import org.jfoundry.test.archunit.fixture.hexagonalconventions.valid.port.out.HelpDocumentReadModelPort;

@SecondaryAdapter
public class MybatisHelpDocumentReadModelAdapter implements HelpDocumentReadModelPort {

    @Override
    public String findTitle() {
        return "title";
    }
}

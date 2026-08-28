package vn.gpg.unionportal;

import org.junit.jupiter.api.Test;
import vn.gpg.unionportal.validation.MemberProfileCatalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

class MemberProfileCatalogTests {
    @Test
    void acceptsOnlyCompanyAndWorkplaceValuesDefinedByTheReferenceWorkbook() {
        assertThat(MemberProfileCatalog.companies()).hasSize(18)
                .contains("CÔNG TY CỔ PHẦN DỊCH VỤ KỸ THUẬT AZ");
        assertThat(MemberProfileCatalog.workplaces()).hasSize(29)
                .contains("BSD", "VP-TCT");
        assertThatNoException().isThrownBy(() -> MemberProfileCatalog.validate(
                "CÔNG TY CỔ PHẦN DỊCH VỤ KỸ THUẬT AZ", "BSD"));

        assertThatIllegalArgumentException().isThrownBy(() -> MemberProfileCatalog.validate("Công ty tự nhập", "BSD"))
                .withMessageContaining("Công ty");
        assertThatIllegalArgumentException().isThrownBy(() -> MemberProfileCatalog.validate(null, "Nơi làm việc tự nhập"))
                .withMessageContaining("Nơi làm việc");
    }
}

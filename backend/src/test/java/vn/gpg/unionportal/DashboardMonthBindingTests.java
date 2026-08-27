package vn.gpg.unionportal;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.gpg.unionportal.controller.WelfareController;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.service.WelfareService;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardMonthBindingTests {
    @Test
    void listEndpointBindsTheDashboardMonthIntoListQuery() throws Exception {
        WelfareService service = mock(WelfareService.class);
        when(service.search(any(ListQuery.class))).thenReturn(List.of());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new WelfareController(service)).build();

        mvc.perform(get("/api/welfare").param("all", "true").param("month", "2026-08"))
                .andExpect(status().isOk());

        ArgumentCaptor<ListQuery> query = ArgumentCaptor.forClass(ListQuery.class);
        verify(service).search(query.capture());
        assertThat(query.getValue().fetchAll()).isTrue();
        assertThat(query.getValue().monthValue()).isEqualTo(YearMonth.of(2026, 8));
    }
}

package contactapp.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link JsonErrorReportValve}.
 *
 * <p>Tests the Tomcat valve that intercepts errors at the container level
 * and returns JSON responses instead of HTML error pages.
 */
class JsonErrorReportValveTest {

    private JsonErrorReportValve valve;
    private Request mockRequest;
    private Response mockResponse;
    private ServletOutputStream mockOutputStream;

    @BeforeEach
    void setUp() throws IOException {
        valve = new JsonErrorReportValve();
        mockRequest = mock(Request.class);
        mockResponse = mock(Response.class);
        mockOutputStream = mock(ServletOutputStream.class);

        when(mockResponse.getOutputStream()).thenReturn(mockOutputStream);
    }

    @Test
    void report_successfulResponse_doesNotWriteBody() throws IOException {
        when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK);
        when(mockResponse.isCommitted()).thenReturn(false);

        valve.report(mockRequest, mockResponse, null);

        verify(mockResponse, never()).setContentType(any());
    }

    @Test
    void report_committedResponse_doesNotWriteBody() throws IOException {
        when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_BAD_REQUEST);
        when(mockResponse.isCommitted()).thenReturn(true);

        valve.report(mockRequest, mockResponse, null);

        verify(mockResponse, never()).setContentType(any());
    }

    @Test
    void report_badRequest_writesJsonBody() throws IOException {
        when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_BAD_REQUEST);
        when(mockResponse.isCommitted()).thenReturn(false);

        valve.report(mockRequest, mockResponse, null);

        verify(mockResponse).setContentType("application/json");
        verify(mockResponse).setCharacterEncoding("UTF-8");
        verify(mockResponse).resetBuffer();

        final String expected = "{\"message\":\"Bad request\"}";
        verify(mockResponse).setContentLength(expected.getBytes(StandardCharsets.UTF_8).length);
        verify(mockOutputStream).write(expected.getBytes(StandardCharsets.UTF_8));
        verify(mockOutputStream).flush();
    }

    @Test
    void report_notFound_writesCorrectMessage() throws IOException {
        when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_NOT_FOUND);
        when(mockResponse.isCommitted()).thenReturn(false);

        valve.report(mockRequest, mockResponse, null);

        final String expected = "{\"message\":\"Resource not found\"}";
        verify(mockResponse).setContentLength(expected.getBytes(StandardCharsets.UTF_8).length);
        verify(mockOutputStream).write(expected.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void report_resetBufferThrowsException_returnsEarly() throws IOException {
        when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_BAD_REQUEST);
        when(mockResponse.isCommitted()).thenReturn(false);
        doThrow(new IllegalStateException("Already committed")).when(mockResponse).resetBuffer();

        valve.report(mockRequest, mockResponse, null);

        verify(mockResponse, never()).setContentType(any());
    }

    @Test
    void report_ioException_handledGracefully() throws IOException {
        when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_BAD_REQUEST);
        when(mockResponse.isCommitted()).thenReturn(false);
        when(mockResponse.getOutputStream()).thenThrow(new IOException("Connection reset"));

        // Should not throw
        valve.report(mockRequest, mockResponse, null);
    }

    @ParameterizedTest
    @CsvSource({
            "400, Bad request",
            "401, Unauthorized",
            "403, Forbidden",
            "404, Resource not found",
            "405, Method not allowed",
            "415, Unsupported media type",
            "500, Internal server error",
            "418, Error"  // Unknown status code defaults to "Error"
    })
    void report_statusCodeMapping(final int statusCode, final String expectedMessage)
            throws IOException {
        when(mockResponse.getStatus()).thenReturn(statusCode);
        when(mockResponse.isCommitted()).thenReturn(false);

        valve.report(mockRequest, mockResponse, null);

        final String expected = "{\"message\":\"" + expectedMessage + "\"}";
        verify(mockResponse).setContentLength(expected.getBytes(StandardCharsets.UTF_8).length);
        verify(mockOutputStream).write(expected.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void report_withThrowable_stillWritesJson() throws IOException {
        when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        when(mockResponse.isCommitted()).thenReturn(false);

        valve.report(mockRequest, mockResponse, new RuntimeException("Test exception"));

        verify(mockResponse).setContentType("application/json");
        final String expected = "{\"message\":\"Internal server error\"}";
        verify(mockResponse).setContentLength(expected.getBytes(StandardCharsets.UTF_8).length);
        verify(mockOutputStream).write(expected.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void report_statusBelowThreshold_doesNotWriteBody() throws IOException {
        // 399 is below ERROR_STATUS_THRESHOLD (400)
        when(mockResponse.getStatus()).thenReturn(399);
        when(mockResponse.isCommitted()).thenReturn(false);

        valve.report(mockRequest, mockResponse, null);

        verify(mockResponse, never()).setContentType(any());
    }

    @Test
    void report_exactlyAtThreshold_writesBody() throws IOException {
        // 400 is exactly at ERROR_STATUS_THRESHOLD
        when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_BAD_REQUEST);
        when(mockResponse.isCommitted()).thenReturn(false);

        valve.report(mockRequest, mockResponse, null);

        verify(mockResponse).setContentType("application/json");
    }
}

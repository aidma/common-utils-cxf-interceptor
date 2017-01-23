package ru.aidma.cxf.interceptor;

import lombok.*;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.cxf.phase.Phase.RECEIVE;

@Builder
public class CxfInterceptor extends AbstractSoapInterceptor {

    private final Function<String, String> function;
    private final Charset encoding;
    private final String phase; //Field just for lombok builder

    //For Builder (default values initialization)
    private CxfInterceptor(Function<String, String> function, Charset encoding, String phase) {
        super(phase != null ? phase : RECEIVE);
        this.function = function != null ? function : s -> s;
        this.encoding = encoding != null ? encoding : UTF_8;
        this.phase = getPhase(); // Not important. Can be deleted.
    }

    @Override
    public void handleMessage(SoapMessage soapMessage) {
        try {
            Optional.of(soapMessage.getContent(InputStream.class))
                    .map(this::streamToString)
                    .map(function)
                    .map(this::stringToStream)
                    .ifPresent(stream -> soapMessage.setContent(InputStream.class, stream));
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    private ByteArrayInputStream stringToStream(String s) {
        val bytes = s.getBytes(encoding);
        return new ByteArrayInputStream(bytes);
    }

    @SneakyThrows
    private String streamToString(InputStream is) {
        @Cleanup val bos = new CachedOutputStream();
        // use the appropriate input stream and restore it later
        IOUtils.copyAndCloseInput(is, bos);
        bos.flush();
        return new String(bos.getBytes(), encoding);
    }

}

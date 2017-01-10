package ru.aidma.cxf.interceptor

import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.helpers.IOUtils
import org.apache.cxf.interceptor.Fault
import spock.lang.Specification

import static java.nio.charset.StandardCharsets.UTF_8

class CxfInterceptorSpec extends Specification {
    {
        def encoding = UTF_8
        String.metaClass.asType = { Class clazz -> stringAsStream(delegate, encoding) }
        InputStream.metaClass.asType = { Class clazz -> streamAsString(delegate, encoding) }
    }

    def "handleMessage should work with default params"() {
        setup:
        def text = "123"
        def soapMessage = Mock SoapMessage

        when:
        def interceptor = CxfInterceptor.builder().build()
        interceptor.handleMessage(soapMessage)

        then:
        1 * soapMessage.getContent(InputStream) >> { text as InputStream }
        1 * soapMessage.setContent(InputStream, _)
    }

    def "handleMessage should change soapMessage body"() {
        setup:
        def soapMessage = Mock SoapMessage

        when:
        def interceptor = CxfInterceptor.builder()
                .function(function)
                .build()
        interceptor.handleMessage(soapMessage)

        then:
        1 * soapMessage.getContent(InputStream) >> { text as InputStream }
        1 * soapMessage.setContent(InputStream, _) >> { assert it[1] as String == result }

        where:
        text        | function                       | result
        "123"       | { s -> s }                     | "123"
        "абв"       | { s -> s }                     | "абв"
        "ааабббввв" | { s -> s.replaceAll("а", "") } | "бббввв"
    }

    def "handleMessage should throw only o.a.c.i.Fault"() {
        setup:
        def soapMessage = Mock SoapMessage
        soapMessage.getContent(InputStream) >> { throw exception as Exception }

        when:
        def interceptor = CxfInterceptor.builder().build()
        interceptor.handleMessage(soapMessage)

        then:
        thrown Fault

        where:
        exception << [new RuntimeException(),
                      new Exception(),
                      new Fault(new Exception())]
    }

    def streamAsString(inputStream, encoding) {
        IOUtils.toString((InputStream) inputStream, (String) encoding) //InputStream failed
    }

    def stringAsStream(str, encoding) {
        def bytes = str.getBytes((String) encoding)
        new ByteArrayInputStream((byte[]) bytes)
    }
}

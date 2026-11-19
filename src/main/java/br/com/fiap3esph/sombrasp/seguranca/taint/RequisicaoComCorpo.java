package br.com.fiap3esph.sombrasp.seguranca.taint;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Guarda o corpo da requisição em memória para que ele possa ser lido duas vezes:
 * uma pelo motor de parsing de PII e outra pelo controller.
 */
class RequisicaoComCorpo extends HttpServletRequestWrapper {
    private final byte[] corpo;

    RequisicaoComCorpo(HttpServletRequest request, byte[] corpo) {
        super(request);
        this.corpo = corpo;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream entrada = new ByteArrayInputStream(corpo);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return entrada.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException("Leitura assíncrona não suportada.");
            }

            @Override
            public int read() {
                return entrada.read();
            }

            @Override
            public int read(byte[] b, int off, int len) {
                return entrada.read(b, off, len);
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), charset()));
    }

    @Override
    public int getContentLength() {
        return corpo.length;
    }

    @Override
    public long getContentLengthLong() {
        return corpo.length;
    }

    private Charset charset() {
        String nome = getCharacterEncoding();
        return nome == null ? StandardCharsets.UTF_8 : Charset.forName(nome);
    }
}

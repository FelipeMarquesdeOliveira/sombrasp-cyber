package br.com.fiap3esph.sombrasp.seguranca.taint;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

/** Liga o {@link GuardaTaintConsole} no {@code System.out} e no {@code System.err} quando a aplicação sobe. */
@Component
public class InstaladorGuardaConsole {

    @PostConstruct
    void instalar() {
        if (!(System.out instanceof GuardaTaintConsole)) {
            System.setOut(new GuardaTaintConsole(System.out, "System.out"));
        }
        if (!(System.err instanceof GuardaTaintConsole)) {
            System.setErr(new GuardaTaintConsole(System.err, "System.err"));
        }
    }

    @PreDestroy
    void remover() {
        if (System.out instanceof GuardaTaintConsole guarda) {
            System.setOut(guarda.original());
        }
        if (System.err instanceof GuardaTaintConsole guarda) {
            System.setErr(guarda.original());
        }
    }
}

package com.clubakos.backend.controller;

import com.clubakos.backend.entity.Adjunto;
import com.clubakos.backend.repository.AdjuntoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/adjuntos")
public class AdjuntoController {

    @Autowired
    private AdjuntoRepository repository;

    @GetMapping("/test")
    public Adjunto crearAdjuntoDePrueba() {
        Adjunto nuevo = new Adjunto();
        nuevo.setNombreArchivo("certificado_medico.pdf");
        nuevo.setTipoArchivo("application/pdf");
        nuevo.setDatosExtra("{\"vencimiento\": \"2027-12-31\", \"aprobado\": true}");
        return repository.save(nuevo);
    }
}
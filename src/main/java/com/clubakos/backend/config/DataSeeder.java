package com.clubakos.backend.config;

import com.clubakos.backend.entity.Usuario;
import com.clubakos.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) throws Exception {
        // Solo inyectar datos si la base de datos está vacía
        if (usuarioRepository.count() == 0) {
            cargarUsuariosDesdeCSV("AKOS_Base_de_Datos_Backend_USUARIOS.csv");
            System.out.println("✅ Base de datos inicializada con usuarios en columnas limpias.");
        }
    }

    private void cargarUsuariosDesdeCSV(String filePath) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String line;
        String csvSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Leer y descartar la cabecera
            br.readLine();

            while ((line = br.readLine()) != null) {
                // Separa los campos. El -1 asegura que no se descarten columnas vacías al final
                String[] datos = line.split(csvSplitBy, -1);

                if (datos.length >= 32) {
                    Usuario usuario = new Usuario();

                    usuario.setNombre(limpiarDato(datos[1]));
                    usuario.setTipoUsuario(limpiarDato(datos[2]));
                    usuario.setEmail(limpiarDato(datos[3]));

                    String pinCrudo = limpiarDato(datos[4]);
                    usuario.setPassword(encoder.encode(pinCrudo));

                    usuario.setPlan(limpiarDato(datos[5]));
                    usuario.setEstado(limpiarDato(datos[6]));

                    String clasesStr = limpiarDato(datos[7]);
                    usuario.setClasesPorSemana(clasesStr.isEmpty() ? 0 : Integer.parseInt(clasesStr));

                    usuario.setDni(limpiarDato(datos[8]));
                    usuario.setDireccion(limpiarDato(datos[9]));
                    usuario.setTelefono(limpiarDato(datos[10]));
                    usuario.setFechaNacimiento(limpiarDato(datos[11]));
                    usuario.setNacionalidad(limpiarDato(datos[12]));
                    usuario.setGenero(limpiarDato(datos[13]));
                    usuario.setCiudadEstado(limpiarDato(datos[14]));
                    usuario.setCodigoPostal(limpiarDato(datos[15]));
                    usuario.setContactoEmergencia(limpiarDato(datos[16]));
                    usuario.setParentezco(limpiarDato(datos[17]));
                    usuario.setOcupacion(limpiarDato(datos[18]));
                    usuario.setManoCuerda(limpiarDato(datos[19]));
                    usuario.setLibrasReales(limpiarDato(datos[20]));
                    usuario.setDrawLength(limpiarDato(datos[21]));
                    usuario.setSpineFlecha(limpiarDato(datos[22]));
                    usuario.setBraceHeight(limpiarDato(datos[23]));
                    usuario.setTiller(limpiarDato(datos[24]));
                    usuario.setSetupArco(limpiarDato(datos[25]));
                    usuario.setFechaAlta(limpiarDato(datos[26]));
                    usuario.setCuotaActual(limpiarDato(datos[27]));
                    usuario.setPagoMesCorriente(limpiarDato(datos[28]));
                    usuario.setClubDondeEntrena(limpiarDato(datos[29]));
                    usuario.setFotoPerfil(limpiarDato(datos[30]));
                    usuario.setGrupoVinculados(limpiarDato(datos[31]));

                    // Evita guardar filas completamente vacías
                    if (!usuario.getEmail().isEmpty()) {
                        usuarioRepository.save(usuario);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Error leyendo el archivo CSV: " + e.getMessage());
        }
    }

    private String limpiarDato(String dato) {
        if (dato == null) return "";
        return dato.trim().replace("\"", "");
    }
}
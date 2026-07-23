package com.fenomina.historicos_service.service;

import com.fenomina.historicos_service.dto.notificaciones.NotificacionVacacionesDTO;
import com.fenomina.historicos_service.entity.historical.NotificacionVacaciones;
import com.fenomina.historicos_service.entity.master.Empresa;
import com.fenomina.historicos_service.repository.historical.NotificacionVacacionesRepository;
import com.fenomina.historicos_service.repository.master.EmpleadoRepository;
import com.fenomina.historicos_service.repository.master.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionVacacionesService {

    private static final int DIAS_ALERTA = 30;

    private final NotificacionVacacionesRepository notificacionRepository;
    private final EmpresaRepository                empresaRepository;
    private final EmpleadoRepository               empleadoRepository;

    @Transactional
    public List<NotificacionVacacionesDTO> evaluar() {
        LocalDate hoy            = LocalDate.now();
        LocalDate fechaObjetivo  = hoy.plusDays(DIAS_ALERTA);

        List<Empresa> empresas = empresaRepository.findAllByOrderByNombreEmpresaAsc();
        List<NotificacionVacacionesDTO> nuevas = new ArrayList<>();

        for (Empresa empresa : empresas) {
            List<Object[]> empleados = empleadoRepository
                    .findEmpleadosActivosPorEmpresa(empresa.getEmpresaId());

            for (Object[] emp : empleados) {
                Long      empleadoId  = toLong(emp[0]);
                String    documento   = (String) emp[1];
                String    nombres     = (String) emp[2];
                String    apellidos   = (String) emp[3];
                LocalDate fechaIngreso = toLocalDate(emp[4]);

                if (fechaIngreso == null) continue;

                LocalDate proximaFecha = calcularProximaFechaVac(fechaIngreso, hoy);

                // Solo notificar si faltan exactamente 30 días
                if (!proximaFecha.equals(fechaObjetivo)) continue;

                long diasRestantes = hoy.until(proximaFecha,
                        java.time.temporal.ChronoUnit.DAYS);

                notificacionRepository.insertarSiNoExiste(
                        empleadoId,
                        empresa.getEmpresaId(),
                        empresa.getNombreEmpresa(),
                        documento,
                        nombres,
                        apellidos,
                        proximaFecha,
                        (int) diasRestantes,
                        hoy
                );

                // Traer la notificación recién insertada o ya existente
                nuevas.add(NotificacionVacacionesDTO.builder()
                        .fkEmpleadoId(empleadoId)
                        .fkIdEmpresa(empresa.getEmpresaId())
                        .nombreEmpresa(empresa.getNombreEmpresa())
                        .documentoEmp(documento)
                        .nombresEmp(nombres)
                        .apellidosEmp(apellidos)
                        .proximaFechaVac(proximaFecha)
                        .diasRestantes((int) diasRestantes)
                        .fechaDisparo(hoy)
                        .leida(false)
                        .build());
            }
        }

        log.info("Evaluación completada: {} notificaciones generadas para {}",
                nuevas.size(), hoy);
        return nuevas;
    }

    @Transactional(readOnly = true)
    public List<NotificacionVacacionesDTO> listar(
            String nombreEmpresa, LocalDate desde, LocalDate hasta) {
        return notificacionRepository
                .findByFiltros(
                        nombreEmpresa,
                        desde != null ? desde.toString() : null,
                        hasta != null ? hasta.toString() : null
                )
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas() {
        return notificacionRepository.countByLeidaFalse();
    }

    @Transactional
    public void marcarLeida(Long id) {
        notificacionRepository.findById(id).ifPresent(n -> {
            n.setLeida(true);
            notificacionRepository.save(n);
        });
    }

    @Transactional
    public void marcarTodasLeidas() {
        List<NotificacionVacaciones> noLeidas = notificacionRepository
                .findTodasOrdenadas()
                .stream()
                .filter(n -> !Boolean.TRUE.equals(n.getLeida()))
                .toList();
        noLeidas.forEach(n -> n.setLeida(true));
        notificacionRepository.saveAll(noLeidas);
    }

    private LocalDate calcularProximaFechaVac(LocalDate fechaIngreso, LocalDate hoy) {
        LocalDate candidata;
        try {
            candidata = LocalDate.of(hoy.getYear(),
                    fechaIngreso.getMonth(), fechaIngreso.getDayOfMonth());
        } catch (java.time.DateTimeException e) {
            candidata = LocalDate.of(hoy.getYear(), fechaIngreso.getMonth(), 28);
        }
        if (!candidata.isAfter(hoy)) {
            try {
                candidata = LocalDate.of(hoy.getYear() + 1,
                        fechaIngreso.getMonth(), fechaIngreso.getDayOfMonth());
            } catch (java.time.DateTimeException e) {
                candidata = LocalDate.of(hoy.getYear() + 1,
                        fechaIngreso.getMonth(), 28);
            }
        }
        return candidata;
    }

    private NotificacionVacacionesDTO toDTO(NotificacionVacaciones n) {
        return NotificacionVacacionesDTO.builder()
                .id(n.getId())
                .fkEmpleadoId(n.getFkEmpleadoId())
                .fkIdEmpresa(n.getFkIdEmpresa())
                .nombreEmpresa(n.getNombreEmpresa())
                .documentoEmp(n.getDocumentoEmp())
                .nombresEmp(n.getNombresEmp())
                .apellidosEmp(n.getApellidosEmp())
                .proximaFechaVac(n.getProximaFechaVac())
                .diasRestantes(n.getDiasRestantes())
                .fechaDisparo(n.getFechaDisparo())
                .leida(n.getLeida())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private Long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Long) return (Long) v;
        return ((Number) v).longValue();
    }

    private LocalDate toLocalDate(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDate) return (LocalDate) v;
        if (v instanceof java.sql.Date) return ((java.sql.Date) v).toLocalDate();
        return LocalDate.parse(v.toString());
    }
}
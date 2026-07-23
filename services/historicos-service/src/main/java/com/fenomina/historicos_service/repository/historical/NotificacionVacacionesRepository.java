package com.fenomina.historicos_service.repository.historical;

import com.fenomina.historicos_service.entity.historical.NotificacionVacaciones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NotificacionVacacionesRepository
        extends JpaRepository<NotificacionVacaciones, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO historical.notificaciones_vacaciones
            (fk_empleado_id, fk_id_empresa, nombre_empresa, documento_emp,
             nombres_emp, apellidos_emp, proxima_fecha_vac, dias_restantes,
             fecha_disparo, leida, created_at)
        VALUES
            (:empleadoId, :empresaId, :nombreEmpresa, :documento,
             :nombres, :apellidos, :proximaFecha, :diasRestantes,
             :fechaDisparo, false, NOW())
        ON CONFLICT (fk_empleado_id, proxima_fecha_vac) DO NOTHING
        """, nativeQuery = true)
    void insertarSiNoExiste(
            @Param("empleadoId")     Long      empleadoId,
            @Param("empresaId")      Long      empresaId,
            @Param("nombreEmpresa")  String    nombreEmpresa,
            @Param("documento")      String    documento,
            @Param("nombres")        String    nombres,
            @Param("apellidos")      String    apellidos,
            @Param("proximaFecha")   LocalDate proximaFecha,
            @Param("diasRestantes")  Integer   diasRestantes,
            @Param("fechaDisparo")   LocalDate fechaDisparo
    );

    List<NotificacionVacaciones> findByFechaDisparoOrderByCreatedAtDesc(LocalDate fecha);

    @Query(value = """
    SELECT * FROM historical.notificaciones_vacaciones
    ORDER BY fecha_disparo DESC, created_at DESC
    """, nativeQuery = true)
    List<NotificacionVacaciones> findTodasOrdenadas();

    long countByLeidaFalse();

    @Query(value = """
    SELECT * FROM historical.notificaciones_vacaciones n
    WHERE (:nombreEmpresa IS NULL
           OR LOWER(CAST(n.nombre_empresa AS text))
              LIKE LOWER(CONCAT('%', CAST(:nombreEmpresa AS text), '%')))
      AND (:desde IS NULL OR n.fecha_disparo >= CAST(:desde AS date))
      AND (:hasta IS NULL OR n.fecha_disparo <= CAST(:hasta AS date))
    ORDER BY n.fecha_disparo DESC, n.created_at DESC
    """, nativeQuery = true)
    List<NotificacionVacaciones> findByFiltros(
            @Param("nombreEmpresa") String    nombreEmpresa,
            @Param("desde")         String    desde,
            @Param("hasta")         String    hasta
    );
}

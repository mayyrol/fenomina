-- ============================================================
-- FENOMINA — Script de inicialización de base de datos
-- PostgreSQL 15
-- Ejecutado automáticamente por Docker en primer arranque
-- ============================================================

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

-- ============================================================
-- SCHEMAS
-- ============================================================

CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS historical;
CREATE SCHEMA IF NOT EXISTS master_data;
CREATE SCHEMA IF NOT EXISTS payroll;

-- ============================================================
-- FUNCIÓN DE AUDITORÍA (debe existir antes de los triggers)
-- ============================================================

CREATE OR REPLACE FUNCTION historical.fn_audit_trigger() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
    v_registro_id   BIGINT;
    v_usuario_id    BIGINT;
    v_username      VARCHAR(50);
    v_fk_empresa    BIGINT;
    v_descripcion   TEXT;
    v_datos_ant     JSONB;
    v_datos_new     JSONB;
BEGIN
    IF TG_OP = 'DELETE' THEN
        BEGIN
            v_registro_id := (row_to_json(OLD) ->> (TG_ARGV[0]))::BIGINT;
        EXCEPTION WHEN OTHERS THEN
            v_registro_id := NULL;
        END;
        v_datos_ant := row_to_json(OLD)::JSONB;
        v_datos_new := NULL;
    ELSE
        BEGIN
            v_registro_id := (row_to_json(NEW) ->> (TG_ARGV[0]))::BIGINT;
        EXCEPTION WHEN OTHERS THEN
            v_registro_id := NULL;
        END;
        v_datos_new := row_to_json(NEW)::JSONB;
        v_datos_ant := CASE WHEN TG_OP = 'UPDATE' THEN row_to_json(OLD)::JSONB ELSE NULL END;
    END IF;

    IF TG_OP = 'INSERT' THEN
        BEGIN
            v_usuario_id := (row_to_json(NEW) ->> 'created_by')::BIGINT;
        EXCEPTION WHEN OTHERS THEN
            v_usuario_id := NULL;
        END;
    ELSIF TG_OP = 'UPDATE' THEN
        BEGIN
            v_usuario_id := (row_to_json(NEW) ->> 'updated_by')::BIGINT;
        EXCEPTION WHEN OTHERS THEN
            v_usuario_id := NULL;
        END;
    ELSE
        v_usuario_id := NULL;
    END IF;

    IF v_usuario_id IS NOT NULL THEN
        SELECT user_name INTO v_username
        FROM auth.usuario
        WHERE usuario_id = v_usuario_id;
    END IF;

    BEGIN
        IF TG_OP = 'DELETE' THEN
            v_fk_empresa := (row_to_json(OLD) ->> 'fk_id_empresa')::BIGINT;
        ELSE
            v_fk_empresa := (row_to_json(NEW) ->> 'fk_id_empresa')::BIGINT;
        END IF;
    EXCEPTION WHEN OTHERS THEN
        v_fk_empresa := NULL;
    END;

    v_descripcion := TG_OP || ' en ' || TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME
                     || ' | registro_id: ' || COALESCE(v_registro_id::TEXT, 'N/A')
                     || CASE WHEN v_username IS NOT NULL THEN ' | usuario: ' || v_username ELSE '' END;

    INSERT INTO historical.system_audit_log (
        tabla_afectada, schema_afectado, operacion, registro_id,
        usuario_id, username, fk_id_empresa, datos_anteriores, datos_nuevos, descripcion
    ) VALUES (
        TG_TABLE_NAME, TG_TABLE_SCHEMA, TG_OP, v_registro_id,
        v_usuario_id, v_username, v_fk_empresa, v_datos_ant, v_datos_new, v_descripcion
    );

    RETURN NULL;
END;
$$;

-- ============================================================
-- TABLAS — Schema: auth
-- ============================================================

CREATE TABLE auth.audit_logs (
    id bigint NOT NULL,
    usuario_id bigint,
    username character varying(50),
    accion character varying(50) NOT NULL,
    ip_address character varying(45),
    user_agent text,
    "timestamp" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    detalles text
);

CREATE SEQUENCE auth.audit_logs_id_seq
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE auth.audit_logs_id_seq OWNED BY auth.audit_logs.id;
ALTER TABLE ONLY auth.audit_logs ALTER COLUMN id SET DEFAULT nextval('auth.audit_logs_id_seq'::regclass);

CREATE TABLE auth.refresh_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    token character varying(255) NOT NULL,
    usuario_id bigint NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    revoked boolean DEFAULT false,
    ip_address character varying(45),
    user_agent character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE auth.revinfo_seq
    START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE auth.usuario (
    usuario_id bigint NOT NULL,
    user_name character varying(50) NOT NULL,
    nombres_usuario character varying(255) NOT NULL,
    apellidos_usuario character varying(255) NOT NULL,
    num_identi_usuario character varying(50) NOT NULL,
    cargo_usuario character varying(60) NOT NULL,
    contrasena_usuario character varying(60) NOT NULL,
    fk_id_empresa bigint,
    rol_usuario character varying(20) DEFAULT 'RRHH'::character varying NOT NULL,
    estado_usuario boolean DEFAULT true NOT NULL,
    intentos_fallidos_login integer DEFAULT 0,
    ultimo_login timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    created_by bigint,
    updated_by bigint,
    deleted_at timestamp without time zone,
    fecha_bloqueo timestamp without time zone,
    bloqueado_login boolean DEFAULT false,
    CONSTRAINT chk_rol CHECK (((rol_usuario)::text = ANY ((ARRAY[
        'SUPER_ADMIN'::character varying,
        'RRHH'::character varying,
        'AUDITOR'::character varying,
        'CLIENTE_EMPRESA'::character varying
    ])::text[])))
);

CREATE SEQUENCE auth.usuarios_usuario_id_seq
    AS integer START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE auth.usuarios_usuario_id_seq OWNED BY auth.usuario.usuario_id;
ALTER TABLE ONLY auth.usuario ALTER COLUMN usuario_id SET DEFAULT nextval('auth.usuarios_usuario_id_seq'::regclass);

-- ============================================================
-- TABLAS — Schema: historical
-- ============================================================

CREATE TABLE historical.system_audit_log (
    audit_id bigint NOT NULL,
    tabla_afectada character varying(100) NOT NULL,
    schema_afectado character varying(50) NOT NULL,
    operacion character varying(10) NOT NULL,
    registro_id bigint,
    usuario_id bigint,
    username character varying(50),
    fk_id_empresa bigint,
    datos_anteriores jsonb,
    datos_nuevos jsonb,
    descripcion text,
    ip_address character varying(45),
    "timestamp" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_operacion CHECK (((operacion)::text = ANY ((ARRAY[
        'INSERT'::character varying,
        'UPDATE'::character varying,
        'DELETE'::character varying
    ])::text[])))
);

CREATE SEQUENCE historical.system_audit_log_audit_id_seq
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE historical.system_audit_log_audit_id_seq OWNED BY historical.system_audit_log.audit_id;
ALTER TABLE ONLY historical.system_audit_log ALTER COLUMN audit_id SET DEFAULT nextval('historical.system_audit_log_audit_id_seq'::regclass);

-- ============================================================
-- TABLAS — Schema: master_data
-- ============================================================

CREATE TABLE master_data.periodi_concepto (
    periodi_concepto_id bigint NOT NULL,
    nombre_periodi character varying(30) NOT NULL,
    valor_periodi integer
);

CREATE SEQUENCE master_data.periodi_concepto_periodi_concepto_id_seq
    AS integer START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE master_data.periodi_concepto_periodi_concepto_id_seq OWNED BY master_data.periodi_concepto.periodi_concepto_id;
ALTER TABLE ONLY master_data.periodi_concepto ALTER COLUMN periodi_concepto_id SET DEFAULT nextval('master_data.periodi_concepto_periodi_concepto_id_seq'::regclass);

CREATE TABLE master_data.concepto_nomina (
    concep_nomina_id bigint NOT NULL,
    fk_periodi_concepto_id bigint NOT NULL,
    nombre_concep_nomina character varying(255) NOT NULL,
    descr_concep_nomina text,
    categoria_conc_nomina character varying(50) NOT NULL,
    es_salario boolean DEFAULT false NOT NULL,
    es_ibc boolean DEFAULT false NOT NULL,
    es_informativo boolean DEFAULT false NOT NULL,
    es_variable boolean DEFAULT false NOT NULL,
    tipo_entrada_concept character varying(20) NOT NULL,
    responsable_pago_lic_inca character varying(30),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_categoria CHECK (((categoria_conc_nomina)::text = ANY ((ARRAY[
        'DEVENGO'::character varying,
        'DEDUCCION'::character varying,
        'PROVISION'::character varying,
        'APORTE_PATRONAL'::character varying
    ])::text[]))),
    CONSTRAINT chk_tipo_entrada CHECK (((tipo_entrada_concept)::text = ANY ((ARRAY[
        'HORAS'::character varying,
        'DIAS'::character varying,
        'VALOR_FIJO'::character varying,
        'PORCENTAJE'::character varying
    ])::text[])))
);

CREATE SEQUENCE master_data.concepto_nomina_concep_nomina_id_seq
    AS integer START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE master_data.concepto_nomina_concep_nomina_id_seq OWNED BY master_data.concepto_nomina.concep_nomina_id;
ALTER TABLE ONLY master_data.concepto_nomina ALTER COLUMN concep_nomina_id SET DEFAULT nextval('master_data.concepto_nomina_concep_nomina_id_seq'::regclass);

CREATE TABLE master_data.empresa (
    empresa_id bigint NOT NULL,
    empresa_nit character varying(30) NOT NULL,
    razon_social character varying(70) NOT NULL,
    nombre_empresa character varying(70) NOT NULL,
    es_exonerada_ley1607 boolean DEFAULT false NOT NULL,
    logo_empresa_url text,
    aplica_nomina boolean DEFAULT true NOT NULL,
    aplica_prima boolean DEFAULT true NOT NULL,
    aplica_cesantias boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    deleted_at timestamp without time zone,
    created_by bigint,
    updated_by bigint
);

CREATE SEQUENCE master_data.empresa_empresa_id_seq
    AS integer START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE master_data.empresa_empresa_id_seq OWNED BY master_data.empresa.empresa_id;
ALTER TABLE ONLY master_data.empresa ALTER COLUMN empresa_id SET DEFAULT nextval('master_data.empresa_empresa_id_seq'::regclass);

CREATE TABLE master_data.empleado (
    empleado_id bigint NOT NULL,
    fk_id_empresa bigint NOT NULL,
    tipo_documento character varying(70) NOT NULL,
    documento_emp character varying(30) NOT NULL,
    nombres_emp character varying(255) NOT NULL,
    apellidos_emp character varying(255) NOT NULL,
    direccion_emp character varying(255),
    tipo_contrato_emp character varying(50),
    fecha_ingreso_emp date NOT NULL,
    fecha_retiro_emp date,
    fecha_fin_contrato date,
    cargo_emp character varying(60),
    es_salario_integral boolean DEFAULT false,
    salario_basc_mensual numeric(15,2) NOT NULL,
    tiene_aux_transporte boolean DEFAULT true NOT NULL,
    clase_riesgo character varying(20) NOT NULL,
    tipo_cotizante character varying(60) NOT NULL,
    subtipo_cotizante character varying(60) NOT NULL,
    nombre_arl character varying(30) NOT NULL,
    nombre_eps character varying(30) NOT NULL,
    fondo_pension_emp character varying(60) NOT NULL,
    caja_compensacion character varying(60) NOT NULL,
    fondo_cesantias_emp character varying(60) NOT NULL,
    esta_exnrd_parafis boolean DEFAULT false,
    jornada_trabajo_emp character varying(60),
    estado_emp character varying(20) DEFAULT 'ACTIVO'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    created_by bigint,
    updated_by bigint,
    deleted_at timestamp without time zone,
    CONSTRAINT chk_estado_empleado CHECK (((estado_emp)::text = ANY ((ARRAY[
        'ACTIVO'::character varying,
        'INACTIVO'::character varying,
        'RETIRADO'::character varying
    ])::text[])))
);

CREATE SEQUENCE master_data.empleado_empleado_id_seq
    AS integer START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE master_data.empleado_empleado_id_seq OWNED BY master_data.empleado.empleado_id;
ALTER TABLE ONLY master_data.empleado ALTER COLUMN empleado_id SET DEFAULT nextval('master_data.empleado_empleado_id_seq'::regclass);

CREATE TABLE master_data.contrato_concepto (
    contrato_concep_id bigint NOT NULL,
    fk_empleado_id bigint NOT NULL,
    fk_concep_nomina_id bigint NOT NULL,
    valor_fijo numeric(12,2),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    deleted_at timestamp without time zone
);

CREATE SEQUENCE master_data.contrato_concepto_contrato_concep_id_seq
    AS integer START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE master_data.contrato_concepto_contrato_concep_id_seq OWNED BY master_data.contrato_concepto.contrato_concep_id;
ALTER TABLE ONLY master_data.contrato_concepto ALTER COLUMN contrato_concep_id SET DEFAULT nextval('master_data.contrato_concepto_contrato_concep_id_seq'::regclass);

CREATE TABLE master_data.historial_salario (
    hist_salario_id bigint NOT NULL,
    fk_empleado_id bigint NOT NULL,
    salario_anterior numeric(12,2) NOT NULL,
    salario_actual numeric(12,2) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint
);

CREATE SEQUENCE master_data.historial_salario_hist_salario_id_seq
    AS integer START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE master_data.historial_salario_hist_salario_id_seq OWNED BY master_data.historial_salario.hist_salario_id;
ALTER TABLE ONLY master_data.historial_salario ALTER COLUMN hist_salario_id SET DEFAULT nextval('master_data.historial_salario_hist_salario_id_seq'::regclass);

CREATE TABLE master_data.parametro_general (
    param_general_id bigint NOT NULL,
    nombre_param_general character varying(255) NOT NULL,
    descripcion_param text,
    fecha_param_general date NOT NULL,
    valor_param_general numeric(15,3),
    porcentaje_param_general numeric(7,6),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    CONSTRAINT chk_valor_o_porcentaje CHECK ((
        ((valor_param_general IS NOT NULL) AND (porcentaje_param_general IS NULL)) OR
        ((valor_param_general IS NULL) AND (porcentaje_param_general IS NOT NULL))
    ))
);

CREATE SEQUENCE master_data.parametro_general_param_general_id_seq
    AS integer START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE master_data.parametro_general_param_general_id_seq OWNED BY master_data.parametro_general.param_general_id;
ALTER TABLE ONLY master_data.parametro_general ALTER COLUMN param_general_id SET DEFAULT nextval('master_data.parametro_general_param_general_id_seq'::regclass);

-- ============================================================
-- TABLAS — Schema: payroll
-- ============================================================

CREATE TABLE payroll.proceso_liquidacion (
    proceso_liqui_id bigint NOT NULL,
    fk_usuario_id bigint NOT NULL,
    tipo_proceso character varying(30) NOT NULL,
    estado_proc_nomina character varying(30) DEFAULT 'BORRADOR'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    created_by bigint,
    updated_by bigint,
    anio integer NOT NULL,
    periodo integer NOT NULL,
    fecha_inicio_periodo date,
    fecha_fin_periodo date,
    fk_id_empresa bigint NOT NULL,
    CONSTRAINT chk_estado_proceso CHECK (((estado_proc_nomina)::text = ANY ((ARRAY[
        'BORRADOR'::character varying,
        'CERRADO'::character varying,
        'PENDIENTE_PAGO'::character varying,
        'PAGADO'::character varying,
        'ANULADO'::character varying
    ])::text[]))),
    CONSTRAINT chk_tipo_proceso CHECK (((tipo_proceso)::text = ANY ((ARRAY[
        'NOMINA_MENSUAL'::character varying,
        'NOMINA_QUINCENAL'::character varying,
        'PRIMA_SEMESTRAL'::character varying,
        'CESANTIAS_ANUAL'::character varying,
        'INTERESES_CESANTIAS_ANUAL'::character varying
    ])::text[])))
);

CREATE SEQUENCE payroll.proceso_liquidacion_proceso_liqui_id_seq
    AS integer START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE payroll.proceso_liquidacion_proceso_liqui_id_seq OWNED BY payroll.proceso_liquidacion.proceso_liqui_id;
ALTER TABLE ONLY payroll.proceso_liquidacion ALTER COLUMN proceso_liqui_id SET DEFAULT nextval('payroll.proceso_liquidacion_proceso_liqui_id_seq'::regclass);

CREATE TABLE payroll.cabecera_liqui_prestacion (
    cabe_liqui_prestacion_id bigint NOT NULL,
    fk_proceso_liqui_id bigint NOT NULL,
    anio_liqui_prestacion integer NOT NULL,
    periodo_liqui_prestacion integer NOT NULL,
    finicio_general_liqui_prest date,
    ffinal_general_liqui_prest date,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamp without time zone
);

CREATE SEQUENCE payroll.cabecera_liqui_prestacion_cabe_liqui_prestacion_id_seq
    AS integer START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE payroll.cabecera_liqui_prestacion_cabe_liqui_prestacion_id_seq OWNED BY payroll.cabecera_liqui_prestacion.cabe_liqui_prestacion_id;
ALTER TABLE ONLY payroll.cabecera_liqui_prestacion ALTER COLUMN cabe_liqui_prestacion_id SET DEFAULT nextval('payroll.cabecera_liqui_prestacion_cabe_liqui_prestacion_id_seq'::regclass);

CREATE TABLE payroll.nomina_cabecera (
    cabec_nomina_id bigint NOT NULL,
    fk_empleado_id bigint NOT NULL,
    fk_proceso_liqui_id bigint NOT NULL,
    anio_cabec_nomina integer NOT NULL,
    periodo_coti_nomina integer NOT NULL,
    total_devengado_emp numeric(12,2),
    total_deduccion_emp numeric(12,2),
    neto_nomina_emp numeric(15,2),
    fecha_cierre_nomina timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamp without time zone,
    costo_total_empresa numeric(15,2),
    total_ap_patronales numeric(12,2),
    total_provisiones numeric(15,2),
    ibc_salud numeric(15,2),
    ibc_pension numeric(15,2),
    CONSTRAINT chk_costo_total_valido CHECK ((
        (costo_total_empresa = (
            (COALESCE(neto_nomina_emp, (0)::numeric) + COALESCE(total_ap_patronales, (0)::numeric)) +
            COALESCE(total_provisiones, (0)::numeric)
        )) OR (costo_total_empresa IS NULL)
    ))
);

CREATE SEQUENCE payroll.nomina_cabecera_cabec_nomina_id_seq
    AS integer START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE payroll.nomina_cabecera_cabec_nomina_id_seq OWNED BY payroll.nomina_cabecera.cabec_nomina_id;
ALTER TABLE ONLY payroll.nomina_cabecera ALTER COLUMN cabec_nomina_id SET DEFAULT nextval('payroll.nomina_cabecera_cabec_nomina_id_seq'::regclass);

CREATE TABLE payroll.novedad (
    novedad_id bigint NOT NULL,
    fk_empleado_id bigint NOT NULL,
    fk_concep_nomina_id bigint NOT NULL,
    fecha_novedad date,
    fecha_inicio_ausen date,
    fecha_fin_ausen date,
    tipo_vacacion character varying(60),
    cantidad_dias_novedad integer,
    cantidad_horas_novedad numeric(5,2),
    valor_ref_novedad numeric(12,2),
    observaciones text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp without time zone,
    updated_by bigint,
    anio integer NOT NULL,
    periodo integer NOT NULL,
    proceso_liquid bigint
);

CREATE SEQUENCE payroll.novedad_novedad_id_seq
    AS integer START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE payroll.novedad_novedad_id_seq OWNED BY payroll.novedad.novedad_id;
ALTER TABLE ONLY payroll.novedad ALTER COLUMN novedad_id SET DEFAULT nextval('payroll.novedad_novedad_id_seq'::regclass);

CREATE TABLE payroll.detalle_liqui_prestacion (
    detalle_prestacion_id bigint NOT NULL,
    fk_cabe_liqui_prestacion_id bigint NOT NULL,
    fk_empleado_id bigint NOT NULL,
    fk_concep_nomina_id bigint NOT NULL,
    fecha_inicio_corte_emp date NOT NULL,
    fecha_fin_corte_emp date NOT NULL,
    dias_liquidados_int integer,
    promedio_var_periodo numeric(15,2),
    salario_fijo_momento numeric(15,2),
    base_liqui_total numeric(15,2),
    valor_neto_presta numeric(15,2),
    valor_int_cesantias numeric(15,2),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    promedio_aux_transporte numeric(12,2)
);

CREATE SEQUENCE payroll.detalle_liqui_prestacion_detalle_prestacion_id_seq
    AS integer START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE payroll.detalle_liqui_prestacion_detalle_prestacion_id_seq OWNED BY payroll.detalle_liqui_prestacion.detalle_prestacion_id;
ALTER TABLE ONLY payroll.detalle_liqui_prestacion ALTER COLUMN detalle_prestacion_id SET DEFAULT nextval('payroll.detalle_liqui_prestacion_detalle_prestacion_id_seq'::regclass);

CREATE TABLE payroll.reporte_nomina_detalle (
    nomina_detalle_id bigint NOT NULL,
    fk_concep_nomina_id bigint NOT NULL,
    fk_cabec_nomina_id bigint NOT NULL,
    fk_novedad_id bigint,
    fk_contrato_concep_id bigint,
    cantidad_concept numeric(5,2),
    base_calculo_concept numeric(12,2),
    valor_result_concept numeric(12,2),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    observacion_concept text
);

CREATE SEQUENCE payroll.reporte_nomina_detalle_nomina_detalle_id_seq
    AS integer START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE payroll.reporte_nomina_detalle_nomina_detalle_id_seq OWNED BY payroll.reporte_nomina_detalle.nomina_detalle_id;
ALTER TABLE ONLY payroll.reporte_nomina_detalle ALTER COLUMN nomina_detalle_id SET DEFAULT nextval('payroll.reporte_nomina_detalle_nomina_detalle_id_seq'::regclass);

-- ============================================================
-- PRIMARY KEYS
-- ============================================================

ALTER TABLE ONLY auth.audit_logs ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);
ALTER TABLE ONLY auth.refresh_tokens ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);
ALTER TABLE ONLY auth.refresh_tokens ADD CONSTRAINT refresh_tokens_token_key UNIQUE (token);
ALTER TABLE ONLY auth.usuario ADD CONSTRAINT usuarios_num_identi_usuario_key UNIQUE (num_identi_usuario);
ALTER TABLE ONLY auth.usuario ADD CONSTRAINT usuarios_pkey PRIMARY KEY (usuario_id);
ALTER TABLE ONLY auth.usuario ADD CONSTRAINT usuarios_user_name_key UNIQUE (user_name);

ALTER TABLE ONLY historical.system_audit_log ADD CONSTRAINT system_audit_log_pkey PRIMARY KEY (audit_id);

ALTER TABLE ONLY master_data.concepto_nomina ADD CONSTRAINT concepto_nomina_nombre_concep_nomina_key UNIQUE (nombre_concep_nomina);
ALTER TABLE ONLY master_data.concepto_nomina ADD CONSTRAINT concepto_nomina_pkey PRIMARY KEY (concep_nomina_id);
ALTER TABLE ONLY master_data.contrato_concepto ADD CONSTRAINT contrato_concepto_pkey PRIMARY KEY (contrato_concep_id);
ALTER TABLE ONLY master_data.empleado ADD CONSTRAINT empleado_pkey PRIMARY KEY (empleado_id);
ALTER TABLE ONLY master_data.empleado ADD CONSTRAINT uq_documento_empresa UNIQUE (fk_id_empresa, tipo_documento, documento_emp);
ALTER TABLE ONLY master_data.empresa ADD CONSTRAINT empresa_empresa_nit_key UNIQUE (empresa_nit);
ALTER TABLE ONLY master_data.empresa ADD CONSTRAINT empresa_pkey PRIMARY KEY (empresa_id);
ALTER TABLE ONLY master_data.historial_salario ADD CONSTRAINT historial_salario_pkey PRIMARY KEY (hist_salario_id);
ALTER TABLE ONLY master_data.parametro_general ADD CONSTRAINT parametro_general_pkey PRIMARY KEY (param_general_id);
ALTER TABLE ONLY master_data.periodi_concepto ADD CONSTRAINT periodi_concepto_nombre_periodi_key UNIQUE (nombre_periodi);
ALTER TABLE ONLY master_data.periodi_concepto ADD CONSTRAINT periodi_concepto_pkey PRIMARY KEY (periodi_concepto_id);

ALTER TABLE ONLY payroll.cabecera_liqui_prestacion ADD CONSTRAINT cabecera_liqui_prestacion_pkey PRIMARY KEY (cabe_liqui_prestacion_id);
ALTER TABLE ONLY payroll.detalle_liqui_prestacion ADD CONSTRAINT detalle_liqui_prestacion_pkey PRIMARY KEY (detalle_prestacion_id);
ALTER TABLE ONLY payroll.nomina_cabecera ADD CONSTRAINT nomina_cabecera_pkey PRIMARY KEY (cabec_nomina_id);
ALTER TABLE ONLY payroll.nomina_cabecera ADD CONSTRAINT uq_empleado_proceso UNIQUE (fk_empleado_id, fk_proceso_liqui_id);
ALTER TABLE ONLY payroll.novedad ADD CONSTRAINT novedad_pkey PRIMARY KEY (novedad_id);
ALTER TABLE ONLY payroll.proceso_liquidacion ADD CONSTRAINT proceso_liquidacion_pkey PRIMARY KEY (proceso_liqui_id);
ALTER TABLE ONLY payroll.reporte_nomina_detalle ADD CONSTRAINT reporte_nomina_detalle_pkey PRIMARY KEY (nomina_detalle_id);

-- ============================================================
-- ÍNDICES
-- ============================================================

CREATE INDEX idx_audit_accion ON auth.audit_logs USING btree (accion);
CREATE INDEX idx_audit_ip ON auth.audit_logs USING btree (ip_address);
CREATE INDEX idx_audit_timestamp ON auth.audit_logs USING btree ("timestamp" DESC);
CREATE INDEX idx_audit_usuario ON auth.audit_logs USING btree (usuario_id);
CREATE INDEX idx_refresh_expiration ON auth.refresh_tokens USING btree (expires_at) WHERE (NOT revoked);
CREATE INDEX idx_refresh_token ON auth.refresh_tokens USING btree (token) WHERE (NOT revoked);
CREATE INDEX idx_refresh_usuario ON auth.refresh_tokens USING btree (usuario_id);
CREATE INDEX idx_usuario_empresa ON auth.usuario USING btree (fk_id_empresa) WHERE (deleted_at IS NULL);
CREATE INDEX idx_usuario_rol ON auth.usuario USING btree (rol_usuario) WHERE (deleted_at IS NULL);
CREATE INDEX idx_usuario_username ON auth.usuario USING btree (user_name) WHERE (deleted_at IS NULL);

CREATE INDEX idx_sal_fk_id_empresa ON historical.system_audit_log USING btree (fk_id_empresa);
CREATE INDEX idx_sal_registro_id ON historical.system_audit_log USING btree (registro_id);
CREATE INDEX idx_sal_tabla_afectada ON historical.system_audit_log USING btree (tabla_afectada);
CREATE INDEX idx_sal_timestamp ON historical.system_audit_log USING btree ("timestamp" DESC);
CREATE INDEX idx_sal_usuario_id ON historical.system_audit_log USING btree (usuario_id);

CREATE INDEX idx_concepto_categoria ON master_data.concepto_nomina USING btree (categoria_conc_nomina);
CREATE INDEX idx_concepto_periodicidad ON master_data.concepto_nomina USING btree (fk_periodi_concepto_id);
CREATE INDEX idx_contrato_empleado ON master_data.contrato_concepto USING btree (fk_empleado_id) WHERE (deleted_at IS NULL);
CREATE UNIQUE INDEX uq_empleado_concepto_activo ON master_data.contrato_concepto USING btree (fk_empleado_id, fk_concep_nomina_id) WHERE (deleted_at IS NULL);
CREATE INDEX idx_empleado_documento ON master_data.empleado USING btree (documento_emp) WHERE (deleted_at IS NULL);
CREATE INDEX idx_empleado_empresa ON master_data.empleado USING btree (fk_id_empresa) WHERE (deleted_at IS NULL);
CREATE INDEX idx_empleado_estado ON master_data.empleado USING btree (fk_id_empresa, estado_emp) WHERE (deleted_at IS NULL);
CREATE INDEX idx_empleado_nombres ON master_data.empleado USING btree (nombres_emp, apellidos_emp) WHERE (deleted_at IS NULL);
CREATE INDEX idx_empresa_nit ON master_data.empresa USING btree (empresa_nit) WHERE (deleted_at IS NULL);
CREATE INDEX idx_empresa_nombre ON master_data.empresa USING btree (nombre_empresa) WHERE (deleted_at IS NULL);
CREATE INDEX idx_historial_empleado ON master_data.historial_salario USING btree (fk_empleado_id);
CREATE INDEX idx_historial_fecha ON master_data.historial_salario USING btree (created_at DESC);
CREATE INDEX idx_parametro_fecha ON master_data.parametro_general USING btree (fecha_param_general DESC);

CREATE INDEX idx_nomina_cabecera_empleado ON payroll.nomina_cabecera USING btree (fk_empleado_id);
CREATE INDEX idx_nomina_cabecera_empleado_periodo ON payroll.nomina_cabecera USING btree (fk_empleado_id, anio_cabec_nomina DESC, periodo_coti_nomina DESC);
CREATE INDEX idx_nomina_cabecera_periodo ON payroll.nomina_cabecera USING btree (anio_cabec_nomina, periodo_coti_nomina);
CREATE INDEX idx_nomina_cabecera_proceso ON payroll.nomina_cabecera USING btree (fk_proceso_liqui_id);
CREATE INDEX idx_nomina_detalle_cabecera ON payroll.reporte_nomina_detalle USING btree (fk_cabec_nomina_id);
CREATE INDEX idx_nomina_detalle_concepto ON payroll.reporte_nomina_detalle USING btree (fk_concep_nomina_id);
CREATE INDEX idx_novedad_empleado ON payroll.novedad USING btree (fk_empleado_id);
CREATE INDEX idx_novedad_empleado_periodo ON payroll.novedad USING btree (fk_empleado_id, anio, periodo);
CREATE INDEX idx_novedad_fecha ON payroll.novedad USING btree (fecha_novedad DESC);
CREATE INDEX idx_novedad_pendiente ON payroll.novedad USING btree (anio, periodo) WHERE (proceso_liquid IS NULL);
CREATE INDEX idx_prestacion_cabecera_proceso ON payroll.cabecera_liqui_prestacion USING btree (fk_proceso_liqui_id);
CREATE INDEX idx_prestacion_detalle_cabecera ON payroll.detalle_liqui_prestacion USING btree (fk_cabe_liqui_prestacion_id);
CREATE INDEX idx_prestacion_detalle_empleado ON payroll.detalle_liqui_prestacion USING btree (fk_empleado_id);
CREATE INDEX idx_proceso_empresa_estado ON payroll.proceso_liquidacion USING btree (fk_id_empresa, estado_proc_nomina);
CREATE INDEX idx_proceso_empresa_estado_anio ON payroll.proceso_liquidacion USING btree (fk_id_empresa, estado_proc_nomina, anio);
CREATE INDEX idx_proceso_empresa_periodo ON payroll.proceso_liquidacion USING btree (fk_id_empresa, anio, periodo);
CREATE INDEX idx_proceso_tipo_periodo_estado ON payroll.proceso_liquidacion USING btree (fk_id_empresa, tipo_proceso, anio, periodo, estado_proc_nomina);

-- ============================================================
-- DATOS INICIALES — Orden estricto por dependencias FK
-- ============================================================

-- ------------------------------------------------------------
-- PASO 1: SUPER_ADMIN sin fk_id_empresa (rompe la dependencia circular)
-- ------------------------------------------------------------
INSERT INTO auth.usuario (
    usuario_id, user_name, nombres_usuario, apellidos_usuario,
    num_identi_usuario, cargo_usuario, contrasena_usuario,
    fk_id_empresa, rol_usuario, estado_usuario,
    intentos_fallidos_login, bloqueado_login, created_at
) VALUES (
    1,
    'adminfe',
    'Administrador',
    'Fenomina',
    '000000001',
    'Super Administrador',
    '$2a$12$MSVEYyjUwJGrCBjhwZIcte5omA3gFw4mu03HV5444bSQorraffl7a',
    NULL,
    'SUPER_ADMIN',
    true,
    0,
    false,
    CURRENT_TIMESTAMP
);

-- Ajustar la secuencia para que el siguiente usuario tome ID 2
SELECT setval('auth.usuarios_usuario_id_seq', 1, true);

-- ------------------------------------------------------------
-- PASO 2: Periodicidades de conceptos (sin dependencias)
-- ------------------------------------------------------------
INSERT INTO master_data.periodi_concepto (periodi_concepto_id, nombre_periodi, valor_periodi) VALUES
    (1, 'Semanal', 7),
    (2, 'Catorcenal', 14),
    (3, 'Quincenal', 15),
    (4, 'Mensual', 30),
    (5, 'Semestral', 180),
    (6, 'Anual', 360),
    (7, 'Al término del contrato', NULL),
    (8, 'Eventual', NULL);

SELECT setval('master_data.periodi_concepto_periodi_concepto_id_seq', 8, true);

-- ------------------------------------------------------------
-- PASO 3: Conceptos de nómina (depende de periodi_concepto)
-- IDs fijos — el motor de cálculo depende de ellos
-- ------------------------------------------------------------
INSERT INTO master_data.concepto_nomina (
    concep_nomina_id, fk_periodi_concepto_id, nombre_concep_nomina,
    descr_concep_nomina, categoria_conc_nomina,
    es_salario, es_ibc, es_informativo, es_variable,
    tipo_entrada_concept, responsable_pago_lic_inca, created_at
) VALUES
(1,7,'Salario días trabajados','Pago del salario ordinario proporcional a los días efectivamente laborados en el período.','DEVENGO',true,true,false,true,'DIAS',NULL,CURRENT_TIMESTAMP),
(2,8,'Vacaciones disfrutadas','Provisión y pago del descanso remunerado de 15 días hábiles por año laborado.','DEVENGO',true,true,false,true,'DIAS',NULL,CURRENT_TIMESTAMP),
(3,8,'Vacaciones compensadas en dinero','Pago en dinero de las vacaciones no disfrutadas.','DEVENGO',false,false,false,true,'DIAS',NULL,CURRENT_TIMESTAMP),
(4,8,'Incapacidad por enfermedad general','Reconocimiento económico por incapacidad de origen común.','DEVENGO',true,true,false,true,'DIAS','EMPLEADOR_EPS',CURRENT_TIMESTAMP),
(5,8,'Incapacidad por origen laboral','Reconocimiento económico por accidente de trabajo o enfermedad laboral.','DEVENGO',true,false,true,true,'DIAS','ARL',CURRENT_TIMESTAMP),
(6,8,'Licencia de maternidad','Licencia remunerada de 18 semanas por parto.','DEVENGO',true,true,false,true,'DIAS','EPS',CURRENT_TIMESTAMP),
(7,8,'Licencia de paternidad','Licencia remunerada al padre por nacimiento de hijo.','DEVENGO',true,true,false,true,'DIAS','EPS',CURRENT_TIMESTAMP),
(8,8,'Licencia por calamidad doméstica','Permiso remunerado por eventos graves que afectan al núcleo familiar.','DEVENGO',true,true,false,true,'DIAS','EMPLEADOR',CURRENT_TIMESTAMP),
(9,8,'Licencia por matrimonio','Permiso remunerado otorgado al trabajador con motivo de su matrimonio.','DEVENGO',true,true,false,true,'DIAS','EMPLEADOR',CURRENT_TIMESTAMP),
(10,8,'Licencia Ley ISAAC','Licencia remunerada para padres de hijos con enfermedad grave o crónica.','DEVENGO',true,true,false,true,'DIAS','EMPLEADOR',CURRENT_TIMESTAMP),
(11,8,'Licencia por sufragio','Permiso remunerado para ejercer el derecho al voto.','DEVENGO',true,true,false,true,'DIAS','EMPLEADOR',CURRENT_TIMESTAMP),
(12,8,'Cargos transitorios','Permiso remunerado para el desempeño de cargos públicos o sindicales.','DEVENGO',false,false,false,true,'DIAS','EMPLEADOR',CURRENT_TIMESTAMP),
(13,8,'Citaciones judiciales','Permiso remunerado por citaciones a diligencias judiciales.','DEVENGO',false,false,false,true,'DIAS','EMPLEADOR',CURRENT_TIMESTAMP),
(14,8,'Otros permisos remunerados pactados','Permisos remunerados adicionales acordados.','DEVENGO',true,true,false,true,'DIAS','EMPLEADOR',CURRENT_TIMESTAMP),
(15,8,'Licencias no remuneradas','Ausencia autorizada sin derecho a remuneración.','DEDUCCION',false,false,true,true,'DIAS',NULL,CURRENT_TIMESTAMP),
(16,8,'Beneficios o extralegales no salariales','Pagos extrasalariales que no constituyen salario.','DEVENGO',false,false,false,true,'VALOR_FIJO',NULL,CURRENT_TIMESTAMP),
(17,8,'Comisiones','Pago variable por cumplimiento de metas o ventas.','DEVENGO',true,true,false,true,'VALOR_FIJO',NULL,CURRENT_TIMESTAMP),
(18,7,'Bonificaciones habituales','Bonificaciones pagadas de forma periódica y permanente.','DEVENGO',true,true,false,true,'VALOR_FIJO',NULL,CURRENT_TIMESTAMP),
(19,8,'Bonificaciones ocasionales o por mera liberalidad','Bonificaciones pagadas esporádicamente sin obligación contractual.','DEVENGO',false,false,false,true,'VALOR_FIJO',NULL,CURRENT_TIMESTAMP),
(20,7,'Viáticos permanentes manutención y alojamiento','Viáticos habituales que constituyen salario e IBC.','DEVENGO',true,true,false,true,'VALOR_FIJO',NULL,CURRENT_TIMESTAMP),
(21,7,'Otros pagos que constituyen salario','Pagos habituales pactados contractualmente.','DEVENGO',true,true,false,true,'VALOR_FIJO',NULL,CURRENT_TIMESTAMP),
(22,7,'Auxilio de transporte','Subsidio legal obligatorio. No es salario ni IBC.','DEVENGO',false,false,false,false,'VALOR_FIJO',NULL,CURRENT_TIMESTAMP),
(23,8,'Otros pagos que no constituyen salario permanente','Pagos regulares excluidos de la base salarial.','DEVENGO',false,false,false,true,'VALOR_FIJO',NULL,CURRENT_TIMESTAMP),
(24,8,'Recargo nocturno ordinario','Recargo del 35% por trabajo nocturno.','DEVENGO',true,true,false,true,'HORAS',NULL,CURRENT_TIMESTAMP),
(25,8,'Recargo diurno dominical o festivo','Recargo por trabajo en jornada diurna en domingo o festivo.','DEVENGO',true,true,false,true,'HORAS',NULL,CURRENT_TIMESTAMP),
(26,8,'Recargo nocturno dominical o festivo','Recargo por trabajo nocturno en domingo o festivo.','DEVENGO',true,true,false,true,'HORAS',NULL,CURRENT_TIMESTAMP),
(27,8,'Hora extra diurna ordinaria','Trabajo suplementario diurno lunes a sábado.','DEVENGO',true,true,false,true,'HORAS',NULL,CURRENT_TIMESTAMP),
(28,8,'Hora extra nocturna ordinaria','Trabajo suplementario nocturno lunes a sábado.','DEVENGO',true,true,false,true,'HORAS',NULL,CURRENT_TIMESTAMP),
(29,8,'Hora extra diurna dominical o festiva','Trabajo suplementario en jornada diurna en domingo o festivo.','DEVENGO',true,true,false,true,'HORAS',NULL,CURRENT_TIMESTAMP),
(30,8,'Hora extra nocturna dominical o festiva','Trabajo suplementario en jornada nocturna en domingo o festivo.','DEVENGO',true,true,false,true,'HORAS',NULL,CURRENT_TIMESTAMP),
(31,8,'Otro concepto a devenir salarial','Concepto genérico para otros devengos salariales.','DEVENGO',true,true,false,true,'VALOR_FIJO',NULL,CURRENT_TIMESTAMP),
(32,8,'Otro concepto a devenir no salarial','Concepto genérico para otros devengos no salariales.','DEVENGO',false,false,false,true,'VALOR_FIJO',NULL,CURRENT_TIMESTAMP),
(33,7,'Salud empleado','Descuento obligatorio del 4% del IBC a cargo del trabajador.','DEDUCCION',false,false,false,true,'PORCENTAJE',NULL,CURRENT_TIMESTAMP),
(34,7,'Pensión empleado','Descuento obligatorio del 4% del IBC a cargo del trabajador.','DEDUCCION',false,false,false,true,'PORCENTAJE',NULL,CURRENT_TIMESTAMP),
(35,7,'Aporte fondo de solidaridad pensional empleado','Descuento adicional para el Fondo de Solidaridad Pensional.','DEDUCCION',false,false,false,true,'PORCENTAJE',NULL,CURRENT_TIMESTAMP),
(36,4,'Retención en la fuente','Anticipo del impuesto de renta descontado mensualmente.','DEDUCCION',false,false,false,true,'VALOR_FIJO',NULL,CURRENT_TIMESTAMP),
(37,7,'Otros conceptos a deducir salariales','Deducciones autorizadas por el trabajador sobre su salario.','DEDUCCION',false,false,false,true,'VALOR_FIJO',NULL,CURRENT_TIMESTAMP),
(38,7,'Otros conceptos a deducir no salariales','Descuentos sobre pagos no salariales.','DEDUCCION',false,false,false,true,'VALOR_FIJO',NULL,CURRENT_TIMESTAMP),
(39,6,'Cesantías','Provisión mensual equivalente a 1 mes de salario por año laborado.','PROVISION',false,false,true,true,'VALOR_FIJO',NULL,CURRENT_TIMESTAMP),
(40,5,'Prima de servicios','Prestación social equivalente a 15 días de salario por semestre.','PROVISION',true,true,false,true,'VALOR_FIJO',NULL,CURRENT_TIMESTAMP),
(41,6,'Intereses sobre las cesantías','Interés del 12% anual sobre el saldo de cesantías.','PROVISION',false,false,false,true,'PORCENTAJE',NULL,CURRENT_TIMESTAMP),
(42,4,'Aporte salud empleador','Aporte patronal del 8.5% del IBC.','APORTE_PATRONAL',false,false,false,true,'PORCENTAJE',NULL,CURRENT_TIMESTAMP),
(43,4,'Pensión empleador','Aporte patronal del 12% del IBC.','APORTE_PATRONAL',false,false,false,true,'PORCENTAJE',NULL,CURRENT_TIMESTAMP),
(44,4,'ARL empleador','Aporte patronal a la Administradora de Riesgos Laborales.','APORTE_PATRONAL',false,false,false,true,'PORCENTAJE',NULL,CURRENT_TIMESTAMP),
(45,4,'SENA empleador','Aporte parafiscal del 2% de la nómina mensual.','APORTE_PATRONAL',false,false,false,true,'PORCENTAJE',NULL,CURRENT_TIMESTAMP),
(46,4,'ICBF empleador','Aporte parafiscal del 3% de la nómina mensual.','APORTE_PATRONAL',false,false,false,true,'PORCENTAJE',NULL,CURRENT_TIMESTAMP),
(47,4,'Caja de compensación empleador','Aporte parafiscal del 4% de la nómina mensual.','APORTE_PATRONAL',false,false,false,true,'PORCENTAJE',NULL,CURRENT_TIMESTAMP);

SELECT setval('master_data.concepto_nomina_concep_nomina_id_seq', 47, true);

-- ------------------------------------------------------------
-- PASO 4: Parámetros generales (depende de auth.usuario via created_by)
-- Los IDs se auto-generan desde 1 según lo solicitado
-- ------------------------------------------------------------
INSERT INTO master_data.parametro_general (nombre_param_general, descripcion_param, fecha_param_general, valor_param_general, porcentaje_param_general, created_by) VALUES
('SMMLV','SMMLV 2020','2020-01-01',877803.000,NULL,1),
('SMMLV','SMMLV 2021','2021-01-01',908526.000,NULL,1),
('SMMLV','Salario Mínimo Mensual Legal Vigente 2022','2022-01-01',1000000.000,NULL,1),
('SMMLV','Salario Mínimo Mensual Legal Vigente 2023','2023-01-01',1160000.000,NULL,1),
('SMMLV','Salario Mínimo Mensual Legal Vigente 2024','2024-01-01',1300000.000,NULL,1),
('SMMLV','Salario Mínimo Mensual Legal Vigente 2025','2025-01-01',1423500.000,NULL,1),
('SMMLV','Salario Mínimo Mensual Legal Vigente 2026','2026-01-01',1750905.000,NULL,1),
('AUXILIO_TRANSPORTE','Auxilio de transporte 2020','2020-01-01',102854.000,NULL,1),
('AUXILIO_TRANSPORTE','Auxilio de transporte 2021','2021-01-01',106554.000,NULL,1),
('AUXILIO_TRANSPORTE','Auxilio de Transporte legal vigente 2022','2022-01-01',117172.000,NULL,1),
('AUXILIO_TRANSPORTE','Auxilio de Transporte 2023','2023-01-01',140606.000,NULL,1),
('AUXILIO_TRANSPORTE','Auxilio de Transporte 2024','2024-01-01',162000.000,NULL,1),
('AUXILIO_TRANSPORTE','Auxilio de Transporte 2025','2025-01-01',200000.000,NULL,1),
('AUXILIO_TRANSPORTE','Auxilio de Transporte legal vigente 2026','2026-01-01',249095.000,NULL,1),
('VALOR_UVT','Unidad de Valor Tributario vigente 2026','2026-01-01',52374.000,NULL,1),
('SANCION_MINIMA_DIAN','Sanción mínima DIAN equivalente a 10 UVT vigente 2026','2026-01-01',523740.000,NULL,1),
('TOPE_COTIZACION_IBC','Ingreso Base de Cotización máximo 25 SMMLV 2020','2020-01-01',21945075.000,NULL,1),
('TOPE_COTIZACION_IBC','Ingreso Base de Cotización máximo 25 SMMLV 2021','2021-01-01',22713150.000,NULL,1),
('TOPE_COTIZACION_IBC','Ingreso Base de Cotización máximo 25 SMMLV 2022','2022-01-01',25000000.000,NULL,1),
('TOPE_COTIZACION_IBC','Ingreso Base de Cotización máximo 25 SMMLV 2023','2023-01-01',29000000.000,NULL,1),
('TOPE_COTIZACION_IBC','Ingreso Base de Cotización máximo 25 SMMLV 2024','2024-01-01',32500000.000,NULL,1),
('TOPE_COTIZACION_IBC','Ingreso Base de Cotización máximo 25 SMMLV 2025','2025-01-01',35587500.000,NULL,1),
('TOPE_COTIZACION_IBC','Ingreso Base de Cotización máximo permitido: 25 SMMLV vigente 2026','2026-01-01',43772625.000,NULL,1),
('SALARIO_INTEGRAL_MINIMO','Salario integral mínimo legal: 10 SMMLV base + 30% factor prestacional vigente 2026','2026-01-01',22761765.000,NULL,1),
('JORNADA_MAXIMA_SEMANAL','Jornada Máxima Semanal 2022','2022-01-01',48.000,NULL,1),
('JORNADA_MAXIMA_SEMANAL','Jornada máxima semanal laboral 2024','2024-01-01',47.000,NULL,1),
('JORNADA_MAXIMA_SEMANAL','Jornada máxima semanal laboral desde 16 de julio 2024','2024-07-16',46.000,NULL,1),
('JORNADA_MAXIMA_SEMANAL','Jornada máxima semanal vigente hasta el 14 de julio de 2026','2026-01-01',44.000,NULL,1),
('JORNADA_MAXIMA_SEMANAL','Jornada máxima semanal vigente desde el 15 de julio de 2026','2026-07-15',42.000,NULL,1),
('HORAS_TRABAJADAS_MES','Total horas laborales mensuales vigentes hasta el 14 de julio de 2026','2026-01-01',220.000,NULL,1),
('HORAS_TRABAJADAS_MES','Total horas laborales mensuales vigentes desde el 15 de julio de 2026','2026-07-15',210.000,NULL,1),
('VALOR_HORA_ORDINARIA','Valor hora ordinaria basado en SMMLV / 220 horas. Vigente hasta el 14 de julio de 2026','2026-01-01',7959.000,NULL,1),
('VALOR_HORA_ORDINARIA','Valor hora ordinaria basado en SMMLV / 210 horas. Vigente desde el 15 de julio de 2026','2026-07-15',8338.000,NULL,1),
('RECARGO_NOCTURNO','Porcentaje de recargo nocturno. Vigente desde el 25 de diciembre de 2025','2026-01-01',NULL,0.350000,1),
('RECARGO_DIURNO_DOMINICAL','Porcentaje de recargo dominical/festivo diurno. Vigente hasta el 30 de junio de 2026','2026-01-01',NULL,0.800000,1),
('RECARGO_DIURNO_DOMINICAL','Porcentaje de recargo dominical/festivo diurno. Vigente desde el 1 de julio de 2026','2026-07-01',NULL,0.900000,1),
('RECARGO_NOCTURNO_DOMINICAL','Porcentaje de recargo nocturno dominical. Vigente hasta el 30 de junio de 2026','2026-01-01',NULL,1.150000,1),
('RECARGO_NOCTURNO_DOMINICAL','Porcentaje de recargo nocturno dominical. Vigente desde el 1 de julio de 2026','2026-07-01',NULL,1.250000,1),
('EXTRA_DIURNA','Porcentaje hora extra diurna lunes a sábado. Vigente 2026','2026-01-01',NULL,0.250000,1),
('EXTRA_NOCTURNA','Porcentaje hora extra nocturna lunes a sábado. Vigente 2026','2026-01-01',NULL,0.750000,1),
('EXTRA_DIURNA_DOMINICAL','Porcentaje hora extra diurna dominical. Vigente hasta el 30 de junio de 2026','2026-01-01',NULL,1.050000,1),
('EXTRA_DIURNA_DOMINICAL','Porcentaje hora extra diurna dominical. Vigente desde el 1 de julio de 2026','2026-07-01',NULL,1.150000,1),
('EXTRA_NOCTURNA_DOMINICAL','Porcentaje hora extra nocturna dominical. Vigente hasta el 30 de junio de 2026','2026-01-01',NULL,1.550000,1),
('EXTRA_NOCTURNA_DOMINICAL','Porcentaje hora extra nocturna dominical. Vigente desde el 1 de julio de 2026','2026-07-01',NULL,1.650000,1),
('SALUD_EMPLEADO','Porcentaje aporte salud empleado. Vigente 2026','2026-01-01',NULL,0.040000,1),
('PENSION_EMPLEADO','Porcentaje aporte pensión empleado. Vigente 2026','2026-01-01',NULL,0.040000,1),
('FONDO_SOLIDARIDAD_PENSIONAL_1','FSP: IBC >= 4 y < 16 SMMLV. Vigente 2026','2026-01-01',NULL,0.010000,1),
('FONDO_SOLIDARIDAD_PENSIONAL_2','FSP: IBC >= 16 y < 17 SMMLV. Vigente 2026','2026-01-01',NULL,0.012000,1),
('FONDO_SOLIDARIDAD_PENSIONAL_3','FSP: IBC >= 17 y < 18 SMMLV. Vigente 2026','2026-01-01',NULL,0.014000,1),
('FONDO_SOLIDARIDAD_PENSIONAL_4','FSP: IBC >= 18 y < 19 SMMLV. Vigente 2026','2026-01-01',NULL,0.016000,1),
('FONDO_SOLIDARIDAD_PENSIONAL_5','FSP: IBC >= 19 y < 20 SMMLV. Vigente 2026','2026-01-01',NULL,0.018000,1),
('FONDO_SOLIDARIDAD_PENSIONAL_6','FSP: IBC superior a 20 SMMLV. Vigente 2026','2026-01-01',NULL,0.020000,1),
('SALUD_EMPLEADOR','Porcentaje aporte salud empleador. Vigente 2026','2026-01-01',NULL,0.085000,1),
('PENSION_EMPLEADOR','Porcentaje aporte pensión empleador. Vigente 2026','2026-01-01',NULL,0.120000,1),
('ARL_EMPLEADOR_I','ARL Clase I - Riesgo mínimo. Vigente 2026','2026-01-01',NULL,0.005220,1),
('ARL_EMPLEADOR_II','ARL Clase II - Riesgo bajo. Vigente 2026','2026-01-01',NULL,0.010440,1),
('ARL_EMPLEADOR_III','ARL Clase III - Riesgo medio. Vigente 2026','2026-01-01',NULL,0.024360,1),
('ARL_EMPLEADOR_IV','ARL Clase IV - Riesgo alto. Vigente 2026','2026-01-01',NULL,0.043500,1),
('ARL_EMPLEADOR_V','ARL Clase V - Riesgo máximo. Vigente 2026','2026-01-01',NULL,0.069600,1),
('CAJA_COMPENSACION','Aporte Caja de Compensación. Vigente 2026','2026-01-01',NULL,0.040000,1),
('SENA','Aporte SENA. Vigente 2026','2026-01-01',NULL,0.020000,1),
('ICBF','Aporte ICBF. Vigente 2026','2026-01-01',NULL,0.030000,1),
('PRIMA_SERVICIOS','Tasa mensual provisión prima de servicios. Vigente 2026','2026-01-01',NULL,0.083300,1),
('CESANTIAS','Tasa mensual provisión cesantías. Vigente 2026','2026-01-01',NULL,0.083300,1),
('INTERESES_CESANTIAS','Tasa mensual intereses sobre cesantías. Vigente 2026','2026-01-01',NULL,0.010000,1),
('VACACIONES','Tasa mensual provisión vacaciones. Vigente 2026','2026-01-01',NULL,0.041700,1);

-- ============================================================
-- FOREIGN KEYS — Al final para evitar problemas de orden
-- ============================================================

-- auth
ALTER TABLE ONLY auth.audit_logs
    ADD CONSTRAINT audit_logs_usuario_id_fkey
    FOREIGN KEY (usuario_id) REFERENCES auth.usuario(usuario_id) ON DELETE SET NULL;

ALTER TABLE ONLY auth.refresh_tokens
    ADD CONSTRAINT refresh_tokens_usuario_id_fkey
    FOREIGN KEY (usuario_id) REFERENCES auth.usuario(usuario_id);

ALTER TABLE ONLY auth.usuario
    ADD CONSTRAINT usuarios_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES auth.usuario(usuario_id);

ALTER TABLE ONLY auth.usuario
    ADD CONSTRAINT usuarios_fk_id_empresa_fkey
    FOREIGN KEY (fk_id_empresa) REFERENCES master_data.empresa(empresa_id);

ALTER TABLE ONLY auth.usuario
    ADD CONSTRAINT usuarios_updated_by_fkey
    FOREIGN KEY (updated_by) REFERENCES auth.usuario(usuario_id);

-- master_data
ALTER TABLE ONLY master_data.concepto_nomina
    ADD CONSTRAINT concepto_nomina_fk_periodi_concepto_id_fkey
    FOREIGN KEY (fk_periodi_concepto_id) REFERENCES master_data.periodi_concepto(periodi_concepto_id);

ALTER TABLE ONLY master_data.contrato_concepto
    ADD CONSTRAINT contrato_concepto_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES auth.usuario(usuario_id);

ALTER TABLE ONLY master_data.contrato_concepto
    ADD CONSTRAINT contrato_concepto_fk_concep_nomina_id_fkey
    FOREIGN KEY (fk_concep_nomina_id) REFERENCES master_data.concepto_nomina(concep_nomina_id);

ALTER TABLE ONLY master_data.contrato_concepto
    ADD CONSTRAINT contrato_concepto_fk_empleado_id_fkey
    FOREIGN KEY (fk_empleado_id) REFERENCES master_data.empleado(empleado_id);

ALTER TABLE ONLY master_data.empleado
    ADD CONSTRAINT empleado_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES auth.usuario(usuario_id);

ALTER TABLE ONLY master_data.empleado
    ADD CONSTRAINT empleado_fk_id_empresa_fkey
    FOREIGN KEY (fk_id_empresa) REFERENCES master_data.empresa(empresa_id);

ALTER TABLE ONLY master_data.empleado
    ADD CONSTRAINT empleado_updated_by_fkey
    FOREIGN KEY (updated_by) REFERENCES auth.usuario(usuario_id);

ALTER TABLE ONLY master_data.empresa
    ADD CONSTRAINT empresa_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES auth.usuario(usuario_id);

ALTER TABLE ONLY master_data.empresa
    ADD CONSTRAINT empresa_updated_by_fkey
    FOREIGN KEY (updated_by) REFERENCES auth.usuario(usuario_id);

ALTER TABLE ONLY master_data.historial_salario
    ADD CONSTRAINT historial_salario_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES auth.usuario(usuario_id);

ALTER TABLE ONLY master_data.historial_salario
    ADD CONSTRAINT historial_salario_fk_empleado_id_fkey
    FOREIGN KEY (fk_empleado_id) REFERENCES master_data.empleado(empleado_id);

ALTER TABLE ONLY master_data.parametro_general
    ADD CONSTRAINT parametro_general_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES auth.usuario(usuario_id);

-- payroll
ALTER TABLE ONLY payroll.cabecera_liqui_prestacion
    ADD CONSTRAINT cabecera_liqui_prestacion_fk_proceso_liqui_id_fkey
    FOREIGN KEY (fk_proceso_liqui_id) REFERENCES payroll.proceso_liquidacion(proceso_liqui_id);

ALTER TABLE ONLY payroll.detalle_liqui_prestacion
    ADD CONSTRAINT detalle_liqui_prestacion_fk_cabe_liqui_prestacion_id_fkey
    FOREIGN KEY (fk_cabe_liqui_prestacion_id) REFERENCES payroll.cabecera_liqui_prestacion(cabe_liqui_prestacion_id);

ALTER TABLE ONLY payroll.detalle_liqui_prestacion
    ADD CONSTRAINT detalle_liqui_prestacion_fk_concep_nomina_id_fkey
    FOREIGN KEY (fk_concep_nomina_id) REFERENCES master_data.concepto_nomina(concep_nomina_id);

ALTER TABLE ONLY payroll.detalle_liqui_prestacion
    ADD CONSTRAINT detalle_liqui_prestacion_fk_empleado_id_fkey
    FOREIGN KEY (fk_empleado_id) REFERENCES master_data.empleado(empleado_id);

ALTER TABLE ONLY payroll.proceso_liquidacion
    ADD CONSTRAINT fk_proceso_empresa
    FOREIGN KEY (fk_id_empresa) REFERENCES master_data.empresa(empresa_id);

ALTER TABLE ONLY payroll.nomina_cabecera
    ADD CONSTRAINT nomina_cabecera_fk_empleado_id_fkey
    FOREIGN KEY (fk_empleado_id) REFERENCES master_data.empleado(empleado_id);

ALTER TABLE ONLY payroll.nomina_cabecera
    ADD CONSTRAINT nomina_cabecera_fk_proceso_liqui_id_fkey
    FOREIGN KEY (fk_proceso_liqui_id) REFERENCES payroll.proceso_liquidacion(proceso_liqui_id);

ALTER TABLE ONLY payroll.novedad
    ADD CONSTRAINT novedad_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES auth.usuario(usuario_id);

ALTER TABLE ONLY payroll.novedad
    ADD CONSTRAINT novedad_fk_concep_nomina_id_fkey
    FOREIGN KEY (fk_concep_nomina_id) REFERENCES master_data.concepto_nomina(concep_nomina_id);

ALTER TABLE ONLY payroll.novedad
    ADD CONSTRAINT novedad_fk_empleado_id_fkey
    FOREIGN KEY (fk_empleado_id) REFERENCES master_data.empleado(empleado_id);

ALTER TABLE ONLY payroll.novedad
    ADD CONSTRAINT novedad_proceso_liquid_fkey
    FOREIGN KEY (proceso_liquid) REFERENCES payroll.proceso_liquidacion(proceso_liqui_id);

ALTER TABLE ONLY payroll.novedad
    ADD CONSTRAINT novedad_updated_by_fkey
    FOREIGN KEY (updated_by) REFERENCES auth.usuario(usuario_id);

ALTER TABLE ONLY payroll.proceso_liquidacion
    ADD CONSTRAINT proceso_liquidacion_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES auth.usuario(usuario_id);

ALTER TABLE ONLY payroll.proceso_liquidacion
    ADD CONSTRAINT proceso_liquidacion_fk_usuario_id_fkey
    FOREIGN KEY (fk_usuario_id) REFERENCES auth.usuario(usuario_id);

ALTER TABLE ONLY payroll.proceso_liquidacion
    ADD CONSTRAINT proceso_liquidacion_updated_by_fkey
    FOREIGN KEY (updated_by) REFERENCES auth.usuario(usuario_id);

ALTER TABLE ONLY payroll.reporte_nomina_detalle
    ADD CONSTRAINT reporte_nomina_detalle_fk_cabec_nomina_id_fkey
    FOREIGN KEY (fk_cabec_nomina_id) REFERENCES payroll.nomina_cabecera(cabec_nomina_id);

ALTER TABLE ONLY payroll.reporte_nomina_detalle
    ADD CONSTRAINT reporte_nomina_detalle_fk_concep_nomina_id_fkey
    FOREIGN KEY (fk_concep_nomina_id) REFERENCES master_data.concepto_nomina(concep_nomina_id);

ALTER TABLE ONLY payroll.reporte_nomina_detalle
    ADD CONSTRAINT reporte_nomina_detalle_fk_contrato_concep_id_fkey
    FOREIGN KEY (fk_contrato_concep_id) REFERENCES master_data.contrato_concepto(contrato_concep_id);

ALTER TABLE ONLY payroll.reporte_nomina_detalle
    ADD CONSTRAINT reporte_nomina_detalle_fk_novedad_id_fkey
    FOREIGN KEY (fk_novedad_id) REFERENCES payroll.novedad(novedad_id);

-- ============================================================
-- TRIGGERS DE AUDITORÍA
-- ============================================================

CREATE TRIGGER trg_audit_concepto_nomina
    AFTER INSERT OR DELETE OR UPDATE ON master_data.concepto_nomina
    FOR EACH ROW EXECUTE FUNCTION historical.fn_audit_trigger('concep_nomina_id');

CREATE TRIGGER trg_audit_empleado
    AFTER INSERT OR DELETE OR UPDATE ON master_data.empleado
    FOR EACH ROW EXECUTE FUNCTION historical.fn_audit_trigger('empleado_id');

CREATE TRIGGER trg_audit_empresa
    AFTER INSERT OR DELETE OR UPDATE ON master_data.empresa
    FOR EACH ROW EXECUTE FUNCTION historical.fn_audit_trigger('empresa_id');

CREATE TRIGGER trg_audit_parametro_general
    AFTER INSERT OR DELETE OR UPDATE ON master_data.parametro_general
    FOR EACH ROW EXECUTE FUNCTION historical.fn_audit_trigger('param_general_id');

CREATE TRIGGER trg_audit_nomina_cabecera
    AFTER INSERT OR DELETE OR UPDATE ON payroll.nomina_cabecera
    FOR EACH ROW EXECUTE FUNCTION historical.fn_audit_trigger('cabec_nomina_id');

CREATE TRIGGER trg_audit_novedad
    AFTER INSERT OR DELETE OR UPDATE ON payroll.novedad
    FOR EACH ROW EXECUTE FUNCTION historical.fn_audit_trigger('novedad_id');

CREATE TRIGGER trg_audit_proceso_liquidacion
    AFTER INSERT OR DELETE OR UPDATE ON payroll.proceso_liquidacion
    FOR EACH ROW EXECUTE FUNCTION historical.fn_audit_trigger('proceso_liqui_id');

-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================

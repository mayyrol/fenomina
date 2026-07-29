package com.fenomina.master_data_service.util;

public final class ValidationMessages {

    private ValidationMessages() {
        throw new UnsupportedOperationException("Clase de constantes");
    }

    // Empresa
    public static final String EMPRESA_NIT_REQUIRED = "El NIT de la empresa es obligatorio";
    public static final String EMPRESA_NIT_DUPLICATE = "Ya existe una empresa con este NIT";
    public static final String EMPRESA_RAZON_SOCIAL_REQUIRED = "La razón social es obligatoria";
    public static final String EMPRESA_NOMBRE_REQUIRED = "El nombre de la empresa es obligatorio";
    public static final String EMPRESA_NOT_FOUND = "Empresa no encontrada";
    public static final String EMPRESA_CORREO_DUPLICADO = "Ya existe un correo registrado igual para esta empresa.";

    // Empleado
    public static final String EMPLEADO_DOCUMENTO_REQUIRED = "El documento del empleado es obligatorio";
    public static final String EMPLEADO_DOCUMENTO_DUPLICATE = "Ya existe un empleado con este documento en la empresa";
    public static final String EMPLEADO_NOMBRES_REQUIRED = "Los nombres del empleado son obligatorios";
    public static final String EMPLEADO_APELLIDOS_REQUIRED = "Los apellidos del empleado son obligatorios";
    public static final String EMPLEADO_SALARIO_REQUIRED = "El salario básico es obligatorio";
    public static final String EMPLEADO_SALARIO_INVALID = "El salario básico debe ser mayor a cero";
    public static final String EMPLEADO_FECHA_INGRESO_REQUIRED = "La fecha de ingreso es obligatoria";
    public static final String EMPLEADO_NOT_FOUND = "Empleado no encontrado";
    public static final String EMPLEADO_INVALID_STATE_TRANSITION = "Transición de estado no permitida";

    // Parámetro General
    public static final String PARAMETRO_NOMBRE_REQUIRED = "El nombre del parámetro es obligatorio";
    public static final String PARAMETRO_FECHA_REQUIRED = "La fecha de vigencia es obligatoria";
    public static final String PARAMETRO_VALOR_REQUIRED = "Debe proporcionar valor o porcentaje";
    public static final String PARAMETRO_NOT_FOUND = "Parámetro no encontrado";

    // Concepto Nómina
    public static final String CONCEPTO_NOMBRE_REQUIRED = "El nombre del concepto es obligatorio";
    public static final String CONCEPTO_NOMBRE_DUPLICATE = "Ya existe un concepto con este nombre";
    public static final String CONCEPTO_CATEGORIA_REQUIRED = "La categoría del concepto es obligatoria";
    public static final String CONCEPTO_TIPO_ENTRADA_REQUIRED = "El tipo de entrada es obligatorio";
    public static final String CONCEPTO_NOT_FOUND = "Concepto de nómina no encontrado";

    // Contrato Concepto
    public static final String CONTRATO_CONCEPTO_DUPLICATE = "El empleado ya tiene asignado este concepto";
    public static final String CONTRATO_CONCEPTO_NOT_FOUND = "Contrato de concepto no encontrado";
    public static final String CONTRATO_CONCEPTO_VALOR_REQUIRED = "El valor fijo es obligatorio para este tipo de concepto";

    // Archivo
    public static final String FILE_EMPTY = "El archivo está vacío";
    public static final String FILE_INVALID_EXTENSION = "Extensión de archivo no permitida. Solo se permiten: ";
    public static final String FILE_TOO_LARGE = "El archivo excede el tamaño máximo permitido";
    public static final String FILE_UPLOAD_ERROR = "Error al subir el archivo";
    public static final String FILE_DELETE_ERROR = "Error al eliminar el archivo";

    // Seguridad
    public static final String UNAUTHORIZED = "No tiene permisos para realizar esta operación";
    public static final String FORBIDDEN_EMPRESA_ACCESS = "No tiene acceso a esta empresa";
}

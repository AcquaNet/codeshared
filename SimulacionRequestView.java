package com.sota.application.views.simulacion;


import com.sota.application.constants.*;
import com.sota.application.converters.AmountConverter;
import com.sota.application.data.entity.producto.*;
import com.sota.application.data.model.ActividadFiscal;
import com.sota.application.data.model.SolicitudBean;
import com.sota.application.data.service.ProductoServiceImpl;
import com.sota.application.data.service.UserService;
import com.sota.application.data.service.localServices.ProductoDBServiceImpl;
import com.sota.application.data.service.restServices.SolicitudCalificar.SolicitudCalificarRequestDTO;
import com.sota.application.data.service.restServices.SolicitudCalificar.SolicitudCalificarRestClientService;
import com.sota.application.data.service.restServices.products.ProductoRCServiceImpl;
import com.sota.application.views.MainLayout;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import javax.management.Query;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;


@PageTitle(ConstantsSimulacion.PAGE_TITLE)
@Route(value = ConstantsSimulacion.ROUTE, layout = MainLayout.class)
@RolesAllowed("ADMIN")
@Uses(Icon.class)
public class SimulacionRequestView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    Logger logger = LoggerFactory.getLogger(SimulacionRequestView.class);

    // ------------------------------------------------
    // Services
    // ------------------------------------------------

    private final UserService userService;

    private final SolicitudCalificarRestClientService solicitudCalificarRestClientService;

    private final ProductoServiceImpl productoService;

    // ------------------------------------------------
    // Forms
    // ------------------------------------------------
    private FormLayout hiddenFields;

    // ------------------------------------------------
    // Fields
    // ------------------------------------------------

    private TextField entidadFinanciera;
    private TextField montoSolicitado;

    private ComboBox<Producto> productoComboBox;
    private ComboBox<Plazo> plazoComboBox;
    private ComboBox<Amortizacion> amortizacionComboBox;
    private ComboBox<Periocidad> periocidadComboBox;
    private ComboBox<DestinoFondos> destinoFondosComboBox;
    private ComboBox<PeriodoDeGracia> periodoDeGraciaComboBox;

    private TextField receptor;


    // ------------------------------------------------
    // Buttons
    // ------------------------------------------------
    private Button nextStep;

    private Button confirmStep;

    private Button backStep;

    private Button cancelStep;

    private Button simular;

    // ------------------------------------------------
    // Models
    // ------------------------------------------------

    private final SolicitudBean solicitudBean;

    // ------------------------------------------------
    // Binders
    // ------------------------------------------------

    private Binder<SolicitudBean> solicitudBinder;

    // ------------------------------------------------
    // Parameters
    // ------------------------------------------------

    public SimulacionRequestView(
            UserService userService,
            SolicitudCalificarRestClientService solicitudCalificarRestClientService,
            ProductoServiceImpl productoService,
            @Autowired @RouteScopeOwner(MainLayout.class) SolicitudBean solicitudBean) {


        this.solicitudBean = solicitudBean;

        // -----------------------------------------
        // Assign Service
        // -----------------------------------------

        this.userService = userService;
        this.solicitudCalificarRestClientService = solicitudCalificarRestClientService;
        this.productoService = productoService;

        // -----------------------------------------
        // Configure Page
        // -----------------------------------------
        getContent().setSizeFull();
        getContent().setAlignItems(FlexComponent.Alignment.CENTER);

        // -----------------------------------------
        // Create Field Variables
        // -----------------------------------------

        createFieldVariables();

        // -----------------------------------------
        // Create Binder
        // -----------------------------------------

        createBinders();

        // -----------------------------------------
        // Create Form
        // -----------------------------------------

        getContent().add(createHeader(), createMainFormLayout(), createFooter());

    }

    private void udpateContain() {

        // Inicializa los campos de la pantalla en base a los Models registrados

        this.solicitudBinder.readBean(this.solicitudBean);

    }

    private void createBinders() {

        this.solicitudBinder = new Binder<>(SolicitudBean.class);

        this.solicitudBinder.forField(this.entidadFinanciera)
                .bind("entidad.name");

        this.solicitudBinder.forField(this.montoSolicitado)
                .withConverter(new Converter<String, BigDecimal>() {
                    @Override
                    public Result<BigDecimal> convertToModel(String value, ValueContext context) {

                        try {
                            return Result.ok(AmountConverter.convertToBigDecimal(value));
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }

                    }

                    @Override
                    public String convertToPresentation(BigDecimal value, ValueContext context) {

                        return  AmountConverter.convertToString(value);

                    }
                })
                .bind("montos.montoSolicitado");

        this.solicitudBinder.forField(this.productoComboBox)
               .bind(SolicitudBean::getProducto,SolicitudBean::setProducto);

        this.solicitudBinder.setBean(this.solicitudBean);

    }

    private Component createMainFormLayout() {

        FormLayout formRootLayout = new FormLayout();

        Component formMainLayout = createMainForm();
        Component formAsideLayout = createAsideForm();

        formRootLayout.add(formMainLayout,formAsideLayout);

        formRootLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0px",1),
                new FormLayout.ResponsiveStep("1500px",2));

        return formRootLayout;


    }

    private HorizontalLayout createHeader()
    {

        Span validacion = new Span(Constants.DD_STEPS_01_VALIDACION);
        validacion.getElement().getThemeList().add("badge pill");

        Span aptitudCrediticia = new Span(Constants.DD_STEPS_02_APTIDUD);
        aptitudCrediticia.getElement().getThemeList().add("badge pill");

        Span simularCredito = new Span(Constants.DD_STEPS_03_SIMULAR);
        simularCredito.getElement().getThemeList().add("badge success pill");

        Span resumen = new Span(Constants.DD_STEPS_04_RESUMEN);
        resumen.getElement().getThemeList().add("badge pill");

        HorizontalLayout steps = new HorizontalLayout();
        steps.setWidthFull();
        steps.setAlignItems(FlexComponent.Alignment.START);
        steps.add(validacion,aptitudCrediticia,simularCredito,resumen);

        return steps;


    }

    private Component createMainForm()
    {

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setPadding(true);
        formLayout.setMargin(true);
        formLayout.setWidthFull();
        formLayout.addClassName(LumoUtility.Border.ALL);
        formLayout.addClassName(LumoUtility.BoxShadow.SMALL);

        Component titleInformation =  new H5(ConstantsSimulacion.OPCION_SELECCIONADA_PARA_EL_SOLICITANTE);

        Component titleInput =  new H5(ConstantsSimulacion.COMPLETAR_LOS_SIGUIENTES_CAMPOS_PARA_GENERAR_LA_SIMULACION);

        formLayout.add(titleInformation, createInformationLayout(), titleInput, createInputLayout() , createHiddenFormLayout());

        return formLayout;

    }

    private Component createInformationLayout()
    {
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setWidthFull();
        horizontalLayout.add(this.entidadFinanciera,this.montoSolicitado);
        horizontalLayout.addClassName(LumoUtility.Background.PRIMARY_10);
        horizontalLayout.addClassName(LumoUtility.Border.ALL);
        horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        horizontalLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        horizontalLayout.add(this.entidadFinanciera,this.montoSolicitado);

        return horizontalLayout;

    }

    private Component createInputLayout()
    {
        FormLayout formInputLayout = new FormLayout();
        formInputLayout.setWidthFull();

        formInputLayout.add(this.productoComboBox, this.plazoComboBox, this.amortizacionComboBox, this.periocidadComboBox, this.destinoFondosComboBox, this.periodoDeGraciaComboBox);

        formInputLayout.setColspan(this.productoComboBox,1);
        formInputLayout.setColspan(this.plazoComboBox,1);
        formInputLayout.setColspan(this.amortizacionComboBox,1);
        formInputLayout.setColspan(this.periocidadComboBox,1);
        formInputLayout.setColspan(this.destinoFondosComboBox,1);
        formInputLayout.setColspan(this.periodoDeGraciaComboBox,1);
 
        formInputLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0px",1),
                new FormLayout.ResponsiveStep("1500px",2));

        return formInputLayout;

    }

    private Component createHiddenFormLayout() {

        /* -------------------------------------------------- */
        /* Creacion de Componentes                            */
        /* -------------------------------------------------- */

        this.simular = new Button(ConstantsSimulacion.BUTTON_SIMULAR,new Icon(VaadinIcon.REFRESH));
        this.simular.setIconAfterText(true);
        this.simular.addThemeVariants(ButtonVariant.LUMO_ICON);
        this.simular.addClickListener(event -> processNextStep(event));
        this.simular.setEnabled(false);

        /* -------------------------------------------------- */
        /* Creacion de From                                   */
        /* -------------------------------------------------- */

        this.hiddenFields = new FormLayout();
        this.hiddenFields.setWidthFull();
        this.hiddenFields.setVisible(true);

        this.hiddenFields.setColspan(this.simular,1);

        this.hiddenFields.add(new Hr(), this.simular);

        this.hiddenFields.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0px",1),
                new FormLayout.ResponsiveStep("1500px",2));

        return this.hiddenFields;

    }


    private Component createAsideForm()
    {

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setPadding(true);
        formLayout.setMargin(true);
        formLayout.addClassName(LumoUtility.Background.PRIMARY_50);
        formLayout.addClassName(LumoUtility.Border.ALL);
        formLayout.addClassName(LumoUtility.BoxShadow.SMALL);
        formLayout.addClassName(LumoUtility.TextColor.PRIMARY_CONTRAST);

        Component title =  new H5(ConstantsSimulacion.DD_DATOS_SOLICITANTE);
        title.getStyle().set("color","white");

        Component taxIdTitle =  new H6(ConstantsSimulacion.DD_DATOS_SOLICITANTE_CUIT);
        taxIdTitle.getStyle().set("color","white");
        Span taxId = new Span(this.solicitudBean.getDocumentoFiscal().getDocumentoNro());
        taxId.setWidthFull();

        Component razonSocialTitle =  new H6(ConstantsSimulacion.DD_DATOS_SOLICITANTE_RAZON);
        razonSocialTitle.getStyle().set("color","white");
        Span razonSocial = new Span(this.solicitudBean.getInformacionFiscal().getRazonSocial());
        razonSocial.setWidthFull();

        Component codigoDescripcionTitle =  new H6(ConstantsSimulacion.DD_DATOS_SOLICITANTE_ACTIVIDAD);
        codigoDescripcionTitle.getStyle().set("color","white");
        Span codigoDescripcion = new Span(this.solicitudBean.getActividadFiscal().getDescripcion());
        codigoDescripcion.setWidthFull();

        Component codigoActividadTitle =  new H6(ConstantsSimulacion.DD_DATOS_SOLICITANTE_CODIGO);
        codigoActividadTitle.getStyle().set("color","white");
        Span codigoActividad = new Span(this.solicitudBean.getActividadFiscal().getCodigo());
        codigoActividad.setWidthFull();

        formLayout.add(title,new Span(),taxIdTitle,taxId,new Span(), razonSocialTitle,razonSocial, new Span(),codigoDescripcionTitle, codigoDescripcion,new Span(),codigoActividadTitle,codigoActividad);

        return formLayout;

    }

    private Component createFooter() {

        // ------------------------------------
        // Footer
        // ------------------------------------

        FormLayout footerFormLayout = new FormLayout();

        this.nextStep = new Button(Constants.BUTTON_SIGUIENTE,new Icon(VaadinIcon.ARROW_RIGHT));
        this.nextStep.setIconAfterText(true);
        this.nextStep.addThemeVariants(ButtonVariant.LUMO_ICON);
        this.nextStep.addClickListener(event -> processNextStep(event));
        this.nextStep.setEnabled(false);

        this.confirmStep = new Button(Constants.BUTTON_CONFIRM);
        this.confirmStep.addThemeVariants(ButtonVariant.LUMO_ICON);
        this.confirmStep.addClickListener(event -> processConfirmEntity(event));
        this.confirmStep.setEnabled(true);
        this.confirmStep.setVisible(false);

        this.cancelStep = new Button(Constants.BUTTON_CANCEL);
        this.cancelStep.addThemeVariants(ButtonVariant.LUMO_ICON);
        this.cancelStep.addClickListener(event -> cancelStep());

        Button invisibleButton = new Button();
        invisibleButton.setEnabled(false);
        invisibleButton.getStyle().set( "--vaadin-button-background", "--lumo-base-color");
        invisibleButton.getStyle().set( "--vaadin-button-border", "none");

        this.backStep = new Button(Constants.BUTTON_VOLVER,new Icon(VaadinIcon.ARROW_LEFT));
        this.backStep.addThemeVariants(ButtonVariant.LUMO_ICON);
        this.backStep.addClickListener(event -> closeView());

        // ------------------------------------
        // Create Form and Add Buttons
        // ------------------------------------

        footerFormLayout.add(this.backStep, invisibleButton, this.cancelStep, this.nextStep, this.confirmStep);

        footerFormLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0px",1),
                new FormLayout.ResponsiveStep("1500px",4));

        return footerFormLayout;

    }

    private void processNextStep(ClickEvent<Button> event)
    {

        SolicitudCalificarRequestDTO requestDTO = new SolicitudCalificarRequestDTO();
        requestDTO.setTaxId(this.solicitudBean.getDocumentoFiscal().getDocumentoNro());
        requestDTO.setRazonSocial(this.solicitudBean.getInformacionFiscal().getRazonSocial());
        requestDTO.setNaturalezaJuridica(this.solicitudBean.getInformacionFiscal().getNaturalezaJuridica());
        requestDTO.setActividadDescripcion(this.solicitudBean.getActividadFiscal().getDescripcion());
        requestDTO.setActividadCodigo(this.solicitudBean.getActividadFiscal().getCodigo());
        requestDTO.getSessionHeaderRequestDTO().setJwtToken("100031111");
        requestDTO.getSessionHeaderRequestDTO().setEntidad("E1");
        requestDTO.getSessionHeaderRequestDTO().setTransactionID(10);

        solicitudCalificarRestClientService.validateTaxID(requestDTO, result -> {

            // We now have the results. But, because this call might happen outside normal
            // Vaadin calls, we need to make sure the HTTP Session data of this app isn't
            // violated. For this we use UI#access()
            getUI().get().access(() -> {

                if(result.isValid())
                {

                    this.hiddenFields.setVisible(true);

                    this.nextStep.setVisible(false);

                    this.confirmStep.setVisible(true);

                } else
                {

                    Notification.show(
                            String.format(ConstantsSimulacion.ERROR_OBTENIENDO_CALIFICACION,this.solicitudBean.getDocumentoFiscal().getDocumentoNro()), 3000,
                            Notification.Position.TOP_CENTER);

                    montoSolicitado.setInvalid(true);

                    montoSolicitado.setErrorMessage(String.format(ConstantsSimulacion.ERROR_OBTENIENDO_CALIFICACION,this.solicitudBean.getDocumentoFiscal().getDocumentoNro()));

                    clearAdditionalFields();


                }

            });
        });



    }

    private void processConfirmEntity(ClickEvent<Button> event) {

        try {

            // ---------------------
            // Clear Fields
            // ---------------------

            clearFields();

            // ---------------------
            // Notifications
            // ---------------------

            Notification notification = Notification.show(Constants.OK_ENTITY_ADDED);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            notification.setPosition(Notification.Position.TOP_CENTER);


        } catch (ObjectOptimisticLockingFailureException e)
        {
            logger.debug(Constants.ERROR_ENTITY_LOCKED + ": " + e.getMessage(),e);

            Notification notification = Notification.show(Constants.ERROR_ENTITY_LOCKED);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.setPosition(Notification.Position.TOP_CENTER);

        } catch (DataIntegrityViolationException e) {

            logger.debug(Constants.ERROR_INTEGRITY + ": " + e.getMessage(),e);

            Notification notification = Notification.show(Constants.ERROR_INTEGRITY);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.setPosition(Notification.Position.TOP_CENTER);

        } catch (Exception e) {

            logger.debug(Constants.ERROR_EXCEPTION + ": " + e.getMessage(),e);

            Notification notification = Notification.show(Constants.ERROR_EXCEPTION);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.setPosition(Notification.Position.TOP_CENTER);

        }

    }

    private void clearFields() {

        this.nextStep.setVisible(true);
        this.nextStep.setEnabled(false);
        this.confirmStep.setVisible(false);
        clearAdditionalFields();

    }

    private void setError(String errorMessage) {

        clearAdditionalFields();

        this.montoSolicitado.setInvalid(true);
        this.montoSolicitado.setErrorMessage(errorMessage);

    }

    private void clearAdditionalFields() {

        this.hiddenFields.setVisible(false);

    }

    private void cancelStep() {
        getUI().ifPresent(ui -> { ui.navigate(ConstantsMain.ROUTE);});
    }

    private void closeView() {
        getUI().ifPresent(ui -> {
            ui.navigate(ConstantsCalificacion.ROUTE);});
    }

    private void createFieldVariables() {

        // ------------------------------------------------
        // Entidad Financiera
        // ------------------------------------------------

        this.entidadFinanciera = new TextField();
        this.entidadFinanciera.setReadOnly(true);
        this.entidadFinanciera.setWidthFull();
        this.entidadFinanciera.setPrefixComponent(VaadinIcon.INSTITUTION.create());
        this.entidadFinanciera.getStyle().set("--vaadin-input-field-readonly-border", "0px");
        this.entidadFinanciera.getStyle().set("--vaadin-input-field-height", "--lumo-size-xl");
        this.entidadFinanciera.getStyle().set("--vaadin-input-field-background", "--lumo-success-color-50pct");

        // ------------------------------------------------
        // Monto Solicitado
        // ------------------------------------------------

        this.montoSolicitado = new TextField();
        this.montoSolicitado.setLabel(ConstantsSimulacion.MONTO_SOLICITADO_PES);
        if (this.solicitudBean.getMontos().getMoneda().equals(ConstantsSimulacion.DD_DOLAR))
        {
            this.montoSolicitado.setLabel(ConstantsSimulacion.MONTO_SOLICITADO_USD);
        }

        this.montoSolicitado.setReadOnly(true);
        this.montoSolicitado.setPrefixComponent(VaadinIcon.DOLLAR.create());
        this.montoSolicitado.getStyle().set("--vaadin-input-field-readonly-border", "0px");
        this.montoSolicitado.getStyle().set("--vaadin-input-field-height", "--lumo-size-xl");
        this.montoSolicitado.getStyle().set("--vaadin-input-field-background", "--lumo-success-color-50pct");

        // ------------------------------------------------
        // Productos
        // ------------------------------------------------


        this.productoComboBox = new ComboBox<Producto>(ConstantsSimulacion.DD_TIPO_DE_PRODUCTO);
        this.productoComboBox.setRequired(true);
        this.productoComboBox.setRequiredIndicatorVisible(true);
        this.productoComboBox.setWidth("50%");
        this.productoComboBox.setItemLabelGenerator(Producto::getDenominacion);
        this.productoComboBox.addValueChangeListener(event -> {

            if(event.isFromClient())
            {

                // -----------------------------------------------------------
                // Obtener el Producto Seleccionado
                // -----------------------------------------------------------

                Producto prodd = event.getValue();

                Producto productoSeleccionado = productoService.getById(event.getValue().getId());

                // -----------------------------------------------------------
                // Determinar si el Producto viene de servicio RestAPI
                // -----------------------------------------------------------

                getUI().get().access(() -> {

                    this.productoComboBox.setInvalid(false);
                    this.plazoComboBox.setItems(productoSeleccionado.getPlazos());
                    this.amortizacionComboBox.setItems(productoSeleccionado.getAmortizacionesList());
                    this.periocidadComboBox.setItems(productoSeleccionado.getPeriocidadesList());
                    this.destinoFondosComboBox.setItems(productoSeleccionado.getDestinosFondosList());
                    this.periodoDeGraciaComboBox.setItems(productoSeleccionado.getPeriodoDeGraciaListDeGracia());

                    if(productoSeleccionado.getPlazos().size()==1)
                    {
                        plazoComboBox.setValue(productoSeleccionado.getPlazos().get(0));
                    }

                    if(productoSeleccionado.getAmortizacionesList().size()==1)
                    {
                        amortizacionComboBox.setValue(productoSeleccionado.getAmortizacionesList().get(0));
                    }

                    if(productoSeleccionado.getPeriocidadesList().size()==1)
                    {
                        periocidadComboBox.setValue(productoSeleccionado.getPeriocidadesList().get(0));
                    }

                    if(productoSeleccionado.getDestinosFondosList().size()==1)
                    {
                        destinoFondosComboBox.setValue(productoSeleccionado.getDestinosFondosList().get(0));
                    }

                    if(productoSeleccionado.getPeriodoDeGraciaListDeGracia().size()==1)
                    {
                        periodoDeGraciaComboBox.setValue(productoSeleccionado.getPeriodoDeGraciaListDeGracia().get(0));
                    }


                });

                this.receptor.setValue("1");

            }

        });

        this.productoComboBox.setItemsWithFilterConverter(query -> productoService.findAll(
                query.getFilter().orElse(""), PageRequest.of(query.getPage(), query.getLimit())).stream(), productoComboBox -> productoComboBox);

        this.productoComboBox.setPageSize(40);

        // ------------------------------------------------
        // Plazos
        // ------------------------------------------------

        this.plazoComboBox = new ComboBox<Plazo>(ConstantsSimulacion.DD_PLAZO);
        this.plazoComboBox.setRequiredIndicatorVisible(true);
        this.plazoComboBox.setWidthFull();
        this.plazoComboBox.setItemLabelGenerator(Plazo::getDescripcion);
        this.plazoComboBox.setItems(new ArrayList<>());
        this.plazoComboBox.addValueChangeListener(event -> {

            if(event.isFromClient())
            {

                this.receptor.setValue("2");

            }

        });

        // ------------------------------------------------
        // Amortizacion
        // ------------------------------------------------

        this.amortizacionComboBox = new ComboBox<Amortizacion>(ConstantsSimulacion.DD_AMORTIZACION);
        this.amortizacionComboBox.setRequiredIndicatorVisible(true);
        this.amortizacionComboBox.setWidth("50%");
        this.amortizacionComboBox.setItemLabelGenerator(Amortizacion::getDescripcion);
        this.amortizacionComboBox.setItems(new ArrayList<>());
        this.amortizacionComboBox.addValueChangeListener(event -> {

            if(event.isFromClient())
            {

                this.receptor.setValue("3");

            }

        });

        // ------------------------------------------------
        // Periocidad
        // ------------------------------------------------

        this.periocidadComboBox = new ComboBox<Periocidad>(ConstantsSimulacion.DD_PERIOCIDAD);
        this.periocidadComboBox.setRequiredIndicatorVisible(true);
        this.periocidadComboBox.setWidth("50%");
        this.periocidadComboBox.setItemLabelGenerator(Periocidad::getDescripcion);
        this.periocidadComboBox.setItems(new ArrayList<>());
        this.periocidadComboBox.addValueChangeListener(event -> {

            if(event.isFromClient())
            {

                this.receptor.setValue("4");

            }

        });

        // ------------------------------------------------
        // Destino de Fondos
        // ------------------------------------------------

        this.destinoFondosComboBox = new ComboBox<DestinoFondos>(ConstantsSimulacion.DD_DESTINO_DE_FONDOS);
        this.destinoFondosComboBox.setRequiredIndicatorVisible(true);
        this.destinoFondosComboBox.setWidth("50%");
        this.destinoFondosComboBox.setItemLabelGenerator(DestinoFondos::getDescripcion);
        this.destinoFondosComboBox.setItems(new ArrayList<>());
        this.destinoFondosComboBox.addValueChangeListener(event -> {

            if(event.isFromClient())
            {

                this.receptor.setValue("5");

            }

        });

        // ------------------------------------------------
        // Periodo de Gracias
        // ------------------------------------------------

        this.periodoDeGraciaComboBox = new ComboBox<PeriodoDeGracia>(ConstantsSimulacion.DD_PERIODO_DE_GRACIA);
        this.periodoDeGraciaComboBox.setRequiredIndicatorVisible(true);
        this.periodoDeGraciaComboBox.setWidth("50%");
        this.periodoDeGraciaComboBox.setItemLabelGenerator(PeriodoDeGracia::getDescripcion);
        this.periodoDeGraciaComboBox.setItems(new ArrayList<>());
        this.periodoDeGraciaComboBox.addValueChangeListener(event -> {

            if(event.isFromClient())
            {

                this.receptor.setValue("6");

            }

        });

        // ------------------------------------------------
        // Cambio de Estado
        // ------------------------------------------------

        this.receptor = new TextField();
        this.receptor.setVisible(false);
        this.receptor.addValueChangeListener(textFieldStringComponentValueChangeEvent -> {

                this.simular.setEnabled(!this.productoComboBox.isEmpty() &&
                        !this.amortizacionComboBox.isEmpty() &&
                        !this.plazoComboBox.isEmpty() &&
                        !this.periocidadComboBox.isEmpty() &&
                        !this.destinoFondosComboBox.isEmpty() &&
                        !this.periodoDeGraciaComboBox.isEmpty());

        });


    }

    private void processProductSelection(ClickEvent<Button> event)
    {



    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {

        if(this.solicitudBean==null || this.solicitudBean.getDocumentoFiscal().getDocumentoNro().isEmpty())
        {
            beforeEnterEvent.rerouteTo(ConstantsMain.ROUTE);
        }

        RouteParameters parameters = beforeEnterEvent.getRouteParameters();

        udpateContain();

    }

}

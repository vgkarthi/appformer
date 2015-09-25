/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.livespark.formmodeler.editor.client.editor;

import com.google.gwt.user.client.ui.IsWidget;
import org.guvnor.common.services.shared.metadata.MetadataService;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.ioc.client.container.IOC;
import org.kie.workbench.common.widgets.metadata.client.KieEditor;
import org.kie.workbench.common.widgets.metadata.client.KieEditorView;
import org.livespark.formmodeler.editor.client.editor.events.FieldDroppedEvent;
import org.livespark.formmodeler.editor.client.editor.events.FieldRemovedEvent;
import org.livespark.formmodeler.editor.client.editor.rendering.DraggableFieldComponent;
import org.livespark.formmodeler.editor.client.resources.i18n.Constants;
import org.livespark.formmodeler.editor.client.type.FormDefinitionResourceType;
import org.livespark.formmodeler.editor.model.FieldDefinition;
import org.livespark.formmodeler.editor.model.FormDefinition;
import org.livespark.formmodeler.editor.model.FormModelerContent;
import org.livespark.formmodeler.editor.service.FormEditorService;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.client.annotations.WorkbenchEditor;
import org.uberfire.client.annotations.WorkbenchMenu;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.ext.layout.editor.api.editor.LayoutTemplate;
import org.uberfire.ext.layout.editor.client.LayoutEditor;
import org.uberfire.ext.layout.editor.client.components.LayoutDragComponent;
import org.uberfire.ext.layout.editor.client.components.LayoutDragComponentGroup;
import org.uberfire.ext.plugin.client.perspective.editor.layout.editor.HTMLLayoutDragComponent;
import org.uberfire.ext.widgets.common.client.common.BusyIndicatorView;
import org.uberfire.lifecycle.OnStartup;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.workbench.model.menu.Menus;
import org.uberfire.workbench.type.FileNameUtil;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pefernan on 7/7/15.
 */
@Dependent
@WorkbenchEditor(identifier = "LSFormEditor", supportedTypes = { FormDefinitionResourceType.class })
public class FormEditorPresenter extends KieEditor {

    public interface FormEditorView extends KieEditorView {

        void initDataHoldersPopup( List<String> availableDataHolders );

        public void init(FormEditorPresenter presenter);
        public void setupLayoutEditor(LayoutEditor layoutEditor);
    }

    @Inject
    private LayoutEditor layoutEditor;

    @Inject
    private HTMLLayoutDragComponent htmlLayoutDragComponent;

    @Inject
    private Caller<FormEditorService> editorService;

    @Inject
    private Caller<MetadataService> metadataService;

    @Inject
    private FormDefinitionResourceType resourceType;

    @Inject
    private BusyIndicatorView busyIndicatorView;

    @Inject
    private FormEditorHelper editorContext;

    private FormEditorView view;


    @Inject
    public FormEditorPresenter( FormEditorView baseView ) {
        super( baseView );
        view = baseView;
    }

    @OnStartup
    public void onStartup( final ObservablePath path,
            final PlaceRequest place ) {

        init(path, place, resourceType);
    }

    @Override
    protected void loadContent() {
        editorService.call( new RemoteCallback<FormModelerContent>() {
            @Override
            public void callback( FormModelerContent content ) {
                doLoadContent( content );
            }
        }, getNoSuchFileExceptionErrorCallback() ).loadContent(versionRecordManager.getCurrentPath());
    }

    @Override
    protected void save( String commitMessage ) {
        editorContext.getFormDefinition().setLayoutTemplate( layoutEditor.getLayout() );
        editorService.call( getSaveSuccessCallback(editorContext.getContent().hashCode())  ).save(versionRecordManager.getCurrentPath(),
                editorContext.getContent(),
                metadata,
                commitMessage);
    }

    public void doLoadContent( FormModelerContent content ) {
        busyIndicatorView.hideBusyIndicator();

        // TODO: fix this, this return avoids to reload the layout editor again
        if (editorContext.getContent() != null) return;

        editorContext.setContent( content );

        layoutEditor.init(content.getDefinition().getName(), getLayoutComponents());

        if (content.getDefinition().getLayoutTemplate() == null) content.getDefinition().setLayoutTemplate( new LayoutTemplate(  ) );

        loadAvailableFields(content);

        layoutEditor.loadLayout(content.getDefinition().getLayoutTemplate());

        resetEditorPages(content.getOverview());

        view.init( this );

        view.setupLayoutEditor(layoutEditor);
    }

    protected List<LayoutDragComponent> getLayoutComponents() {

        List<LayoutDragComponent>  list = new ArrayList<LayoutDragComponent>();
        list.add(htmlLayoutDragComponent);

        return list;
    }

    @WorkbenchPartTitle
    public String getTitleText() {
        String fileName = FileNameUtil.removeExtension( versionRecordManager.getCurrentPath(), resourceType );
        return Constants.INSTANCE.form_modeler_title( fileName );
    }

    @WorkbenchMenu
    public Menus getMenus() {
        if ( menus == null ) {
            makeMenuBar();
        }
        return menus;
    }

    @WorkbenchPartView
    public IsWidget getWidget() {
        return super.getWidget();
    }

    protected void makeMenuBar() {
        menus = menuBuilder
                .addSave(versionRecordManager.newSaveMenuItem(new Command() {
                    @Override
                    public void execute() {
                        onSave();
                    }
                }))
                .addCopy(versionRecordManager.getCurrentPath(),
                        fileNameValidator)
                .addRename(versionRecordManager.getPathToLatest(),
                        fileNameValidator)
                .addDelete(versionRecordManager.getPathToLatest())
                .addNewTopLevelMenu(versionRecordManager.buildMenu())
                .build();
    }

    public LayoutTemplate getFormTemplate() {
        return layoutEditor.getLayout();
    }

    public FormDefinition getFormDefinition() {
        return editorContext.getFormDefinition();
    }

    public void getAvailableDataObjectsList() {
        editorService.call( new RemoteCallback<List<String>>() {
            @Override
            public void callback( List<String> availableDataObjects ) {
                view.initDataHoldersPopup( availableDataObjects );
            }
        } ).getAvailableDataObjects( versionRecordManager.getCurrentPath() );
    }

    public void addDataHolder( final String name, final String type ) {
        if ( editorContext.addDataHolder(name, type) ) {
            editorService.call( new RemoteCallback<List<FieldDefinition>>() {
                @Override
                public void callback( List<FieldDefinition> fields ) {
                    addAvailableFields( name, fields );
                }
            } ).getAvailableFieldsForType( editorContext.getContent().getPath(), name, type );
        }
    }

    private void loadAvailableFields( FormModelerContent content ) {
        if ( content.getAvailableFields() == null ) return;

        for ( String holderName : content.getAvailableFields().keySet() ) {
            List<FieldDefinition> availableFields = content.getAvailableFields().get( holderName );
            addAvailableFields( holderName, availableFields );
        }
    }

    protected void addAvailableFields(String holderName, List<FieldDefinition> fields) {
        editorContext.addAvailableFields( fields );

        LayoutDragComponentGroup group = new LayoutDragComponentGroup( holderName );

        for ( FieldDefinition field : fields ) {
            DraggableFieldComponent dragComponent = IOC.getBeanManager().lookupBean( DraggableFieldComponent.class ).getInstance();
            if (dragComponent != null) {
                dragComponent.init( getFormDefinition().getId(), field, editorContext.getContent().getPath() );
                group.addLayoutDragComponent( field.getName(), dragComponent );
            }
        }

        layoutEditor.addDraggableComponentGroup( group );
    }

    protected void onFieldDropped(@Observes FieldDroppedEvent event) {
        if (event.getFormId().equals( editorContext.getFormDefinition().getId() )) {
            FieldDefinition field = editorContext.getFormField( event.getFieldName() );
            if (field != null) {
                layoutEditor.removeDraggableGroupComponent( field.getModelName(), field.getName() );
            }
        }
    }

    protected void onFieldRemoved(@Observes FieldRemovedEvent event) {
        if (event.getFormId().equals( editorContext.getFormDefinition().getId() )) {
            FieldDefinition field = editorContext.removeField( event.getFieldName() );
            if (field != null) {
                DraggableFieldComponent dragComponent = IOC.getBeanManager().lookupBean( DraggableFieldComponent.class ).getInstance();
                if (dragComponent != null) {
                    dragComponent.init( getFormDefinition().getId(), field, editorContext.getContent().getPath() );
                    layoutEditor.addDraggableComponentToGroup( field.getModelName(), field.getName(), dragComponent );
                }
            }
        }
    }
}

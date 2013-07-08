/*
 * Copyright 2012 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.uberfire.client.workbench.panels.impl;

import javax.enterprise.context.Dependent;
import javax.inject.Named;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import org.uberfire.client.workbench.panels.WorkbenchPanelView;
import org.uberfire.client.workbench.part.WorkbenchPartPresenter;
import org.uberfire.workbench.model.PanelDefinition;
import org.uberfire.workbench.model.PartDefinition;
import org.uberfire.workbench.model.Position;

/**
 * A Workbench panel that can contain WorkbenchParts.
 */
@Dependent
@Named("SimpleView")
public class StaticWorkbenchPanelView
        extends BaseWorkbenchPanelView<StaticWorkbenchPanelPresenter> {

    private SimpleResize panel = new SimpleResize();

    public StaticWorkbenchPanelView() {

        panel.addFocusHandler( new FocusHandler() {
            @Override
            public void onFocus( FocusEvent event ) {
                panelManager.onPanelFocus( presenter.getDefinition() );
            }
        } );

//        //When a tab is selected ensure content is resized and set focus
//        panel.addSelectionHandler( new SelectionHandler<PartDefinition>() {
//            @Override
//            public void onSelection( SelectionEvent<PartDefinition> event ) {
//                presenter.onPartLostFocus();
//                presenter.onPartFocus( event.getSelectedItem() );
//            }
//        } );

        initWidget( panel );
    }

    @Override
    public void init( final StaticWorkbenchPanelPresenter presenter ) {
        this.presenter = presenter;
    }

    @Override
    public StaticWorkbenchPanelPresenter getPresenter() {
        return this.presenter;
    }

    @Override
    public void clear() {
        panel.clear();
    }

    @Override
    public void addPart( final WorkbenchPartPresenter.View view ) {
        panel.setWidget( view );
    }

    @Override
    public void addPanel( final PanelDefinition panel,
                          final WorkbenchPanelView view,
                          final Position position ) {
        throw new IllegalArgumentException( "Unhandled Position. Expect subsequent errors." );
    }

    @Override
    public void changeTitle( final PartDefinition part,
                             final String title,
                             final IsWidget titleDecoration ) {
    }

    @Override
    public void selectPart( final PartDefinition part ) {
    }

    @Override
    public void removePart( final PartDefinition part ) {
        panel.clear();
    }

    @Override
    public void setFocus( boolean hasFocus ) {
        //panel.setFocus( hasFocus );
    }

    @Override
    public void onResize() {
        final Widget parent = getParent();
        final int width = parent.getOffsetWidth();
        final int height = parent.getOffsetHeight();
        setPixelSize( width, height );
        presenter.onResize( width, height );
        panel.setPixelSize( width, height );
        super.onResize();
    }

    private class SimpleResize extends SimplePanel implements RequiresResize {

        private SimpleResize() {
        }

        private SimpleResize( final Widget child ) {
            super( child );
        }

        private SimpleResize( final Element elem ) {
            super( elem );
        }

        @Override
        public void onResize() {
            final int width = getParent().getOffsetWidth();
            final int height = getParent().getOffsetHeight();
            getWidget().setPixelSize( width, height );
        }

        public void addFocusHandler( FocusHandler focusHandler ) {
            //To change body of created methods use File | Settings | File Templates.
        }

    }

}
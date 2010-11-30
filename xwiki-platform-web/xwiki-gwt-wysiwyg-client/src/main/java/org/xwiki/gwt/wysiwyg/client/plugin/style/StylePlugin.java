/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.gwt.wysiwyg.client.plugin.style;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractStatefulPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import org.xwiki.gwt.wysiwyg.client.plugin.style.exec.BlockStyleNameExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.style.exec.InlineStyleNameExecutable;

import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.OptGroupElement;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.ListBox;

/**
 * Enhances the editor with the ability to apply predefined styles to the current text selection. Installs a list box on
 * the tool bar with the available styles.
 * 
 * @version $Id$
 */
public class StylePlugin extends AbstractStatefulPlugin implements ChangeHandler
{
    /**
     * Command used to apply a given style name to each of the text nodes from the current text selection.
     */
    private static final Command INLINE_STYLE_NAME = new Command("inlineStyleName");

    /**
     * Command used to apply a given style name to each of the block nodes touched by the current text selection.
     */
    private static final Command BLOCK_STYLE_NAME = new Command("blockStyleName");

    /**
     * The CSS class name used to mark a style name as selected (i.e. applied on the current text selection).
     */
    private static final String SELECTED = "selected";

    /**
     * The widget used to pick a style name.
     */
    private ListBox styleNamePicker;

    /**
     * We keep a reference to the in-line style group to be able to distinguish between in-line and block styles.
     */
    private OptGroupElement inlineStyleGroup;

    /**
     * User interface extension for the editor tool bar.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#init(RichTextArea, Config)
     */
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        // Register custom executables.
        getTextArea().getCommandManager().registerCommand(INLINE_STYLE_NAME, new InlineStyleNameExecutable(textArea));
        getTextArea().getCommandManager().registerCommand(BLOCK_STYLE_NAME, new BlockStyleNameExecutable(textArea));

        if (getTextArea().getCommandManager().isSupported(INLINE_STYLE_NAME)) {
            initStyleNamePicker();

            registerTextAreaHandlers();
            getUIExtensionList().add(toolBarExtension);
        }
    }

    /**
     * Initialize the style name picker.
     */
    private void initStyleNamePicker()
    {
        styleNamePicker = new ListBox();
        styleNamePicker.setTitle(Strings.INSTANCE.stylePickerTitle());
        styleNamePicker.addStyleName("xStyleNamePicker");
        styleNamePicker.addItem(Strings.INSTANCE.stylePickerLabel(), "");
        saveRegistration(styleNamePicker.addChangeHandler(this));

        OptGroupElement blockStyleGroup = styleNamePicker.getElement().getOwnerDocument().createOptGroupElement();
        blockStyleGroup.setLabel(Strings.INSTANCE.styleBlockGroupLabel());
        styleNamePicker.getElement().appendChild(blockStyleGroup);

        inlineStyleGroup = styleNamePicker.getElement().getOwnerDocument().createOptGroupElement();
        inlineStyleGroup.setLabel(Strings.INSTANCE.styleInlineGroupLabel());
        styleNamePicker.getElement().appendChild(inlineStyleGroup);

        StyleDescriptorJSONParser parser = new StyleDescriptorJSONParser();
        for (StyleDescriptor descriptor : parser.parse(getConfig().getParameter("styleNames", "[]"))) {
            styleNamePicker.addItem(descriptor.getLabel(), descriptor.getName());
            OptGroupElement group = descriptor.isInline() ? inlineStyleGroup : blockStyleGroup;
            group.appendChild(styleNamePicker.getElement().getLastChild());
        }
        styleNamePicker.setSelectedIndex(0);

        toolBarExtension.addFeature("stylename", styleNamePicker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#destroy()
     */
    public void destroy()
    {
        if (styleNamePicker != null) {
            styleNamePicker.removeFromParent();
            styleNamePicker = null;
        }

        toolBarExtension.clearFeatures();

        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChangeHandler#onChange(ChangeEvent)
     */
    public void onChange(ChangeEvent event)
    {
        if (styleNamePicker == event.getSource() && styleNamePicker.isEnabled()) {
            int selectedIndex = styleNamePicker.getSelectedIndex();
            styleNamePicker.setSelectedIndex(0);
            // Ignore the first option which is used as the list box label.
            if (selectedIndex > 0) {
                toggleStyle(SelectElement.as(styleNamePicker.getElement()).getOptions().getItem(selectedIndex));
            }
        }
    }

    /**
     * Toggles the style name corresponding to the given style option.
     * 
     * @param styleOption one of the style option from the style name list box
     */
    private void toggleStyle(OptionElement styleOption)
    {
        String styleName = styleOption.getValue();
        boolean inline = styleOption.getParentNode() == inlineStyleGroup;
        Command command = inline ? INLINE_STYLE_NAME : BLOCK_STYLE_NAME;
        getTextArea().setFocus(true);
        getTextArea().getCommandManager().execute(command, styleName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#update()
     */
    public void update()
    {
        if (styleNamePicker.isEnabled()) {
            String inlineStyleNames = getTextArea().getCommandManager().getStringValue(INLINE_STYLE_NAME);
            String blockStyleNames = getTextArea().getCommandManager().getStringValue(BLOCK_STYLE_NAME);
            String mixedStyleNames = (inlineStyleNames + " " + blockStyleNames).trim();
            Set<String> appliedStyleNames = new HashSet<String>(Arrays.asList(mixedStyleNames.split("\\s+")));
            NodeList<OptionElement> options = SelectElement.as(styleNamePicker.getElement()).getOptions();
            // Skip the first option because it is used as the label of the style name picker.
            for (int i = 1; i < options.getLength(); i++) {
                OptionElement option = options.getItem(i);
                if (appliedStyleNames.contains(option.getValue())) {
                    option.addClassName(SELECTED);
                } else {
                    option.removeClassName(SELECTED);
                }
            }
        }
    }
}

/**
 * Copyright (c) 2008 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 * 
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation. 
 */
package com.redhat.rhn.frontend.action.kickstart.tree;

import com.redhat.rhn.domain.channel.Channel;
import com.redhat.rhn.domain.channel.ChannelFactory;
import com.redhat.rhn.domain.kickstart.KickstartFactory;
import com.redhat.rhn.domain.kickstart.KickstartInstallType;
import com.redhat.rhn.domain.kickstart.KickstartableTree;
import com.redhat.rhn.frontend.action.BaseEditAction;
import com.redhat.rhn.frontend.struts.RequestContext;
import com.redhat.rhn.manager.PersistOperation;
import com.redhat.rhn.manager.kickstart.tree.BaseTreeEditOperation;

import org.apache.struts.action.DynaActionForm;
import org.apache.struts.util.LabelValueBean;

import java.util.Iterator;
import java.util.List;

/**
 * TreeCreate class for creating Kickstart Trees
 * @version $Rev: 1 $
 */
public abstract class BaseTreeAction extends BaseEditAction {
    
    public static final String INSTALL_TYPE = "installtype";
    public static final String BASE_PATH = "basepath";
    public static final String BOOT_IMAGE = "bootimage";
    public static final String CHANNEL_ID = "channelid";
    public static final String CHANNELS = "channels";
    public static final String KICKSTART_RPMS = "kickstartrpms";
    public static final String INSTALLTYPES = "installtypes";
    public static final String LABEL = "label";
    public static final String NOCHANNELS = "nochannels";
    public static final String NOAUTOKS = "noautoks";
    public static final String NOINSTALLTYPES = "noinstalltypes";
    public static final String RHN_KICKSTART = "rhnkickstart";
    public static final String HIDE_SUBMIT = "hidesubmit";

    protected void processRequestAttributes(RequestContext rctx, PersistOperation opr) {
        BaseTreeEditOperation bte = (BaseTreeEditOperation) opr;
        Iterator i = bte.getKickstartableChannels().iterator();
        if (!i.hasNext()) {
            rctx.getRequest().setAttribute(NOCHANNELS, "true");
            rctx.getRequest().setAttribute(NOAUTOKS, "true");
            rctx.getRequest().setAttribute(HIDE_SUBMIT, "true");
            return;
        }
        else {
            rctx.getRequest().setAttribute(CHANNELS, 
                    createLabelValueList(i, "getName", "getId"));
        }
        
        Channel selectedBaseChannel = getSelectedBaseChannel(rctx);
        if (selectedBaseChannel == null) {
            rctx.getRequest().setAttribute(NOAUTOKS, "true");
            rctx.getRequest().setAttribute(HIDE_SUBMIT, "true");
            return;
        }
        
        i = bte.getKickstartPackageNamesForChannel(selectedBaseChannel).iterator();
        if (!i.hasNext()) {
            rctx.getRequest().setAttribute(NOAUTOKS, "true");
            rctx.getRequest().setAttribute(HIDE_SUBMIT, "true");
        }
        else {
            rctx.getRequest().setAttribute(KICKSTART_RPMS, 
                    createLabelValueList(i, "getName", "getName"));
        }
        
        i = KickstartFactory.lookupKickstartInstallTypes().iterator();
        if (!i.hasNext()) {
            rctx.getRequest().setAttribute(NOINSTALLTYPES, "true");
            rctx.getRequest().setAttribute(HIDE_SUBMIT, "true");
        }
        else {
            rctx.getRequest().setAttribute(INSTALLTYPES, 
                    createLabelValueList(i, "getName", "getLabel"));
        }
    }

    protected void processCommandSetters(PersistOperation operation, DynaActionForm form) {
        BaseTreeEditOperation bte = (BaseTreeEditOperation) operation;
        bte.setBasePath(form.getString(BASE_PATH));
        bte.setBootImage(form.getString(BOOT_IMAGE));
        Long channelId = (Long) form.get(CHANNEL_ID);
        Channel c = ChannelFactory.lookupByIdAndUser(channelId, operation.getUser());
        bte.setChannel(c);
        KickstartInstallType type = KickstartFactory.
            lookupKickstartInstallTypeByLabel(form.getString(INSTALL_TYPE));
        bte.setInstallType(type);
        bte.setLabel(form.getString(LABEL));
    }
    
    /**
     * Return the selected base channel, either the existing value for a
     * channel being edited, the previous selection on the form after an
     * error, or the first value in the list.
     * @param rctx Request context to examine.
     * @return Channel selected previously or by default, null if neither exist.
     */
    protected Channel getSelectedBaseChannel(RequestContext rctx) {
        
        String previousChannelIdSelection = rctx.getParam(CHANNEL_ID, false);
        if (previousChannelIdSelection != null) {
            return ChannelFactory.lookupById(new Long(previousChannelIdSelection));
        }
        
        KickstartableTree tree = (KickstartableTree)rctx.getRequest().getAttribute(
                RequestContext.KSTREE);
        if (tree != null) {
            // Looks like we're editing an existing tree:
            return tree.getChannel();
        }
        
        List channelLabels = (List)rctx.getRequest().getAttribute(CHANNELS);
        if (channelLabels != null) {
            String channelId = ((LabelValueBean)channelLabels.get(0)).getValue();
            return ChannelFactory.lookupById(new Long(channelId));
        }
        
        return null;
    }
}

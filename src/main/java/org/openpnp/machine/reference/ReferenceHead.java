/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference;

import java.util.ArrayList;

import javax.swing.Action;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.psh.ActuatorsPropertySheetHolder;
import org.openpnp.machine.reference.psh.CamerasPropertySheetHolder;
import org.openpnp.machine.reference.psh.NozzlesPropertySheetHolder;
import org.openpnp.machine.reference.wizards.ReferenceHeadConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractHead;
import org.openpnp.spi.base.SimplePropertySheetHolder;
import org.pmw.tinylog.Logger;

public class ReferenceHead extends AbstractHead {
    protected ReferenceMachine machine;
    protected ReferenceDriver driver;

    public ReferenceHead() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                machine = (ReferenceMachine) configuration.getMachine();
                driver = machine.getDriver();
            }
        });
    }

    @Override
    public void home() throws Exception {
        Logger.debug("{}.home()", getName());
        driver.home(this);
        machine.fireMachineHeadActivity(this);
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceHeadConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        ArrayList<PropertySheetHolder> children = new ArrayList<>();
        children.add(new NozzlesPropertySheetHolder(this, "Nozzles", getNozzles(), null));
        children.add(new CamerasPropertySheetHolder(this, "Cameras", getCameras(), null));
        children.add(new ActuatorsPropertySheetHolder(this, "Actuators", getActuators(), null));
        children.add(new SimplePropertySheetHolder("Paste Dispensers", getPasteDispensers()));
        return children.toArray(new PropertySheetHolder[] {});
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard())};
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    @Override
    public void moveToSafeZ(double speed) throws Exception {
        Logger.debug("{}.moveToSafeZ({})", getName(), speed);
        super.moveToSafeZ(speed);
    }
    
    public void moveTo(ReferenceHeadMountable hm, Location location, double speed) throws Exception {
        if (isSoftLimitsEnabled()) {
            /**
             * Since minLocation and maxLocation are captured with the Camera's coordinates, we need
             * to know where the Camera will land, not the HeadMountable.
             */
            Location cameraLocation = location.subtract(hm.getHeadOffsets());
            cameraLocation = cameraLocation.add(((ReferenceCamera) getDefaultCamera()).getHeadOffsets());
            Location minLocation = this.minLocation.convertToUnits(cameraLocation.getUnits());
            Location maxLocation = this.maxLocation.convertToUnits(cameraLocation.getUnits());
            if (cameraLocation.getX() < minLocation.getX() || cameraLocation.getX() > maxLocation.getX() ||
                    cameraLocation.getY() < minLocation.getY() || cameraLocation.getY() > maxLocation.getY()) {
                throw new Exception(String.format("Can't move %s to %s, outside of soft limits on head %s.",
                        hm.getName(), location, getName()));
            }
        }
        getDriver().moveTo(hm, location, speed);
        getMachine().fireMachineHeadActivity(this);
    }

    @Override
    public String toString() {
        return getName();
    }
    
    ReferenceDriver getDriver() {
        return getMachine().getDriver();
    }
    
    public ReferenceMachine getMachine() {
        return (ReferenceMachine) Configuration.get().getMachine();
    }
}

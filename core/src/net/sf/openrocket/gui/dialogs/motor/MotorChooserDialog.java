package net.sf.openrocket.gui.dialogs.motor;


import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import net.sf.openrocket.gui.dialogs.motor.thrustcurve.ThrustCurveMotorSelectionPanel;
import net.sf.openrocket.gui.util.GUIUtil;
import net.sf.openrocket.l10n.Translator;
import net.sf.openrocket.motor.Motor;
import net.sf.openrocket.motor.ThrustCurveMotor;
import net.sf.openrocket.startup.Application;

public class MotorChooserDialog extends JDialog implements CloseableDialog {
	
	private final ThrustCurveMotorSelectionPanel selectionPanel;
	
	private boolean okClicked = false;
	private static final Translator trans = Application.getTranslator();
	
	
	public MotorChooserDialog(Motor current, double delay, double diameter, Window owner) {
		super(owner, trans.get("MotorChooserDialog.title"), Dialog.ModalityType.APPLICATION_MODAL);
		
		
		JPanel panel = new JPanel(new MigLayout("fill"));
		
		selectionPanel = new ThrustCurveMotorSelectionPanel((ThrustCurveMotor) current, delay, diameter);
		
		panel.add(selectionPanel, "grow, wrap para");
		
		
		// OK / Cancel buttons
		JButton okButton = new JButton(trans.get("dlg.but.ok"));
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				close(true);
			}
		});
		panel.add(okButton, "tag ok, spanx, split");
		
		//// Cancel button
		JButton cancelButton = new JButton(trans.get("dlg.but.cancel"));
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				close(false);
			}
		});
		panel.add(cancelButton, "tag cancel");
		
		this.add(panel);
		
		this.setModal(true);
		this.pack();
		this.setLocationByPlatform(true);
		GUIUtil.setDisposableDialogOptions(this, okButton);
		
		JComponent focus = selectionPanel.getDefaultFocus();
		if (focus != null) {
			focus.grabFocus();
		}
		
		// Set the closeable dialog after all initialization
		selectionPanel.setCloseableDialog(this);
	}
	
	
	/**
	 * Return the motor selected by this chooser dialog, or <code>null</code> if the selection has been aborted.
	 * 
	 * @return	the selected motor, or <code>null</code> if no motor has been selected or the selection was canceled.
	 */
	public Motor getSelectedMotor() {
		if (!okClicked)
			return null;
		return selectionPanel.getSelectedMotor();
	}
	
	/**
	 * Return the selected ejection charge delay.
	 * 
	 * @return	the selected ejection charge delay.
	 */
	public double getSelectedDelay() {
		return selectionPanel.getSelectedDelay();
	}
	
	
	
	@Override
	public void close(boolean ok) {
		okClicked = ok;
		this.setVisible(false);
		
		Motor selected = getSelectedMotor();
		if (okClicked && selected != null) {
			selectionPanel.selectedMotor(selected);
		}
	}
	
}

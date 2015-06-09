package org.geogebra.web.web.gui.view.algebra;

import java.util.ArrayList;
import java.util.List;

import org.geogebra.common.euclidian.event.PointerEventType;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoFunction;
import org.geogebra.common.kernel.geos.GeoList;
import org.geogebra.common.kernel.kernelND.GeoCurveCartesianND;
import org.geogebra.common.main.App;
import org.geogebra.common.main.Feature;
import org.geogebra.common.util.AsyncOperation;
import org.geogebra.web.html5.gui.inputfield.AutoCompleteTextFieldW;
import org.geogebra.web.html5.gui.inputfield.HasSymbolPopup;
import org.geogebra.web.html5.gui.inputfield.HistoryPopupW;
import org.geogebra.web.html5.gui.util.BasicIcons;
import org.geogebra.web.html5.gui.util.ClickStartHandler;
import org.geogebra.web.html5.gui.util.ListItem;
import org.geogebra.web.html5.gui.util.UnorderedList;
import org.geogebra.web.html5.main.DrawEquationWeb;
import org.geogebra.web.web.css.GuiResources;
import org.geogebra.web.web.euclidian.EuclidianStyleBarW;
import org.geogebra.web.web.gui.layout.panels.AlgebraDockPanelW;
import org.geogebra.web.web.gui.util.ButtonPopupMenu;

import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;

/**
 * NewRadioButtonTreeItem for creating new formulas in the algebra view
 * 
 * File created by Arpad Fekete
 */
public class NewRadioButtonTreeItem extends RadioButtonTreeItem implements
		HasSymbolPopup, FocusHandler, BlurHandler {

	// How large this number should be (e.g. place on the screen, or
	// scrollable?) Let's allow practically everything

	// clear formula button
	protected PushButton xButton = null;

	// create special formula button (matrix, piecewise function, parametric
	// curve)
	protected PushButton pButton = null;

	private FlowPanel buttonPanel;

	HistoryPopupW historyPopup;
	ButtonPopupMenu specialPopup;
	EquationEditor editor;

	public NewRadioButtonTreeItem(Kernel kern) {
		super(kern);
		editor = new EquationEditor(app, this);


		//should depend on number of previoous elements?
		addHistoryPopup(true);

		buttonPanel = new FlowPanel();
		buttonPanel.addStyleName("AlgebraViewObjectStylebar");

		// code copied from AutoCompleteTextFieldW,
		// with some modifications!
		xButton = new PushButton(new Image(
				GuiResources.INSTANCE.algebra_delete()));
		xButton.getUpHoveringFace().setImage(
				new Image(GuiResources.INSTANCE.algebra_delete_hover()));
		String id = DOM.createUniqueId();
		// textField.setShowSymbolElement(this.XButton.getElement());
		xButton.getElement().setId(id + "_SymbolButton");
		xButton.getElement().setAttribute("data-visible", "false");
		// XButton.getElement().setAttribute("style", "display: none");
		// XButton.setText("X");
		xButton.addStyleName("XButton");
		xButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				DrawEquationWeb.stornoFormulaMathQuillGGB(
				        NewRadioButtonTreeItem.this, seMayLatex);
				NewRadioButtonTreeItem.this.setFocus(true);
				event.stopPropagation();
				// event.preventDefault();
			}
		});

		if (app.has(Feature.ADD_NEW_OBJECT_BUTTON)) {
			pButton = new PushButton(new Image(
					GuiResources.INSTANCE.algebra_new()));
			pButton.getUpHoveringFace().setImage(
					new Image(GuiResources.INSTANCE.algebra_new_hover()));
			pButton.getElement().setAttribute("data-visible", "false");
			pButton.addStyleName("XButtonNeighbour");
			pButton.addClickHandler(new ClickHandler() {

				@Override
				public void onClick(ClickEvent event) {
					if (specialPopup != null) {
						if (EuclidianStyleBarW.CURRENT_POP_UP != specialPopup
								|| !app.wasPopupJustClosed()) {
							if (EuclidianStyleBarW.CURRENT_POP_UP != null) {
								EuclidianStyleBarW.CURRENT_POP_UP.hide();
							}
							EuclidianStyleBarW.CURRENT_POP_UP = specialPopup;

							app.registerPopup(specialPopup);
							specialPopup.showRelativeTo(pButton);
							specialPopup.getFocusPanel().getElement().focus();
						} else {
							specialPopup.setVisible(false);
							EuclidianStyleBarW.CURRENT_POP_UP = null;
						}
					}
					event.stopPropagation();
					// event.preventDefault();
				}
			});

			specialPopup = new ButtonPopupMenu() {
				@Override
				public void setVisible(boolean visible) {
					super.setVisible(visible);

					// if another button is pressed only the visibility is
					// changed,
					// by firing the event we can react as if it was closed
					CloseEvent.fire(this, this, false);
				}

				@Override
				public void hide() {
					super.hide();
					if (EuclidianStyleBarW.CURRENT_POP_UP.equals(this)) {
						EuclidianStyleBarW.CURRENT_POP_UP = null;
					}
				}
			};
			specialPopup.setAutoHideEnabled(true);
			specialPopup.getPanel().addStyleName("AVmenuListContainer");

			UnorderedList itemList = new UnorderedList();
			itemList.setStyleName("AVmenuListContent");
			specialPopup.getPanel().add(itemList);

			ListItem actual = new ListItem();
			actual.add(new Image(GuiResources.INSTANCE.algebra_new_piecewise()));
			actual.add(new Label(app.getPlain("PiecewiseFunction")));
			actual.addDomHandler(new ClickHandler() {
				public void onClick(ClickEvent ce) {
					specialPopup.setVisible(false);
					EuclidianStyleBarW.CURRENT_POP_UP = null;

					// TODO: only create it in the input bar!!!
					final GeoFunction fun = CondFunRadioButtonTreeItem
							.createBasic(app.getKernel());
					if (fun != null) {
						// in theory, fun is never null, but what if?
						// same code as for matrices, see comments there
						Timer tim = new Timer() {
							public void run() {
								app.getAlgebraView().startEditing(fun);
							}
						};
						tim.schedule(500);
					}
				}
			}, ClickEvent.getType());
			itemList.add(actual);

			actual = new ListItem();
			actual.add(new Image(GuiResources.INSTANCE.algebra_new_matrix()));
			actual.add(new Label(app.getMenu("Matrix")));
			actual.addDomHandler(new ClickHandler() {
				public void onClick(ClickEvent ce) {
					specialPopup.setVisible(false);
					EuclidianStyleBarW.CURRENT_POP_UP = null;

					// TODO: only create it in the input bar!!!
					final GeoList mat = MatrixRadioButtonTreeItem
							.create2x2ZeroMatrix(app.getKernel());
					// scheduleDeferred alone does not work well!
					Timer tim2 = new Timer() {
						public void run() {
							app.getAlgebraView().startEditing(mat);
						}
					};
					// on a good machine, 500ms was usually not enough,
					// but 1000ms was usually enough... however, it turned
					// out this is due to a setTimeout in
					// DrawEquationWeb.drawEquationMathQuillGGB...
					// so we could spare at least 500ms by clearing that timer,
					tim2.schedule(500);

					// but now I'm experimenting with even less timeout, i.e.
					// tim.schedule(200);
					// 200ms is not enough, and as this is a good machine
					// let us say that 500ms is just right, or maybe too little
					// on slow machines -> shall we use scheduleDeferred too?
				}
			}, ClickEvent.getType());
			itemList.add(actual);

			actual = new ListItem();
			actual.add(new Image(GuiResources.INSTANCE.algebra_new_parametric()));
			actual.add(new Label(app.getPlain("CurveCartesian")));
			actual.addDomHandler(new ClickHandler() {
				public void onClick(ClickEvent ce) {
					specialPopup.setVisible(false);
					EuclidianStyleBarW.CURRENT_POP_UP = null;

					// TODO: only create it in the input bar!!!
					final GeoCurveCartesianND curve = ParCurveRadioButtonTreeItem
							.createBasic(app.getKernel());
					if (curve != null) {
						// in theory, fun is never null, but what if?
						// same code as for matrices, see comments there
						Timer tim = new Timer() {
							public void run() {
								app.getAlgebraView().startEditing(curve);
							}
						};
						tim.schedule(500);
					}
				}
			}, ClickEvent.getType());
			itemList.add(actual);
		}

		ClickStartHandler.init(xButton, new ClickStartHandler(false, true) {
			@Override
			public void onClickStart(int x, int y, PointerEventType type) {
				// nothing to do here; just makes sure that
				// event.stopPropagation is called
			}
		});

		if (pButton != null) {
			ClickStartHandler.init(pButton, new ClickStartHandler(false, true) {
				@Override
				public void onClickStart(int x, int y, PointerEventType type) {
					// nothing to do here; just makes sure that
					// event.stopPropagation is called
				}
			});
		}

		try{
			//TRY-CATCH needed for Win8 app //TODO find better solution
			xButton.setFocus(false);
			if (pButton != null) {
				pButton.setFocus(false);
			}
		}catch(Throwable t){
		}
		// add(textField);// done in super()

		// it seems this would be part of the Tree, not of TreeItem...
		// why? web programming knowledge helps: we should add position:
		// relative! to ".GeoGebraFrame .gwt-Tree .gwt-TreeItem .elem"

		add(buttonPanel);// dirty hack of adding it two times!

		if (pButton != null) {
			this.buttonPanel.add(pButton);
		}

		this.buttonPanel.add(xButton);

		// this was necessary earlier in conjuction with add(xButton)
		// ihtml.getElement().appendChild(xButton.getElement());
		// but later this.replaceXButtonDOM() should be used instead

		xButton.getElement().setAttribute("data-visible", "true");
		if (pButton != null) {
			pButton.getElement().setAttribute("data-visible", "true");
		}
		addStyleName("SymbolCanBeShown");

		// When scheduleDeferred does not work...
		// this code makes the cursor show when the page loads...
	}

	public void replaceXButtonDOM() {
		getElement().getParentElement().appendChild(buttonPanel.getElement());
	}

	/**
	 * This is the interface of bringing up a popup of suggestions, from a query
	 * string "sub"... in AutoCompleteTextFieldW, this is supposed to be
	 * triggered automatically by SuggestBox, but in NewRadioButtonTreeItem we
	 * have to call this every time for the actual word in the formula (i.e.
	 * updateCurrentWord(true)), when the formula is refreshed a bit! e.g.
	 * DrawEquationWeb.editEquationMathQuillGGB.onKeyUp or something, so this
	 * will be a method to override!
	 */
	@Override
	public boolean popupSuggestions() {
		return editor.popupSuggestions();
	}

	@Override
	public boolean hideSuggestions() {
		return editor.hideSuggestions();
	}

	/**
	 * In case the suggestion list is showing, shuffle its selected element
	 * up/down, otherwise consider up/down event for the history popup!
	 */
	@Override
	public boolean shuffleSuggestions(boolean down) {
		if (editor.sug.isSuggestionListShowing()) {
			if (down) {
				editor.sug.accessMoveSelectionDown();
			} else {
				editor.sug.accessMoveSelectionUp();
			}
			return false;
		} else if (down) {
			if (historyPopup != null && historyPopup.isDownPopup()) {
				// this would give the focus to the historyPopup,
				// which should catch the key events itself, but maybe it's
				// not everything all right here!
				historyPopup.showPopup();
			} else {
				String text = editor.getNextInput();
				if (text != null) {
					editor.setText(text, true);
				}
			}
		} else {
			if (historyPopup != null && !historyPopup.isDownPopup()) {
				historyPopup.showPopup();
			} else {
				String text = editor.getPreviousInput();
				if (text != null)
					editor.setText(text, true);
			}
		}
		return true;
	}

	@Override
	public boolean stopNewFormulaCreation(String newValue0, String latex,
	        AsyncOperation callback) {
		if (editor.sug.isSuggestionListShowing()) {
			editor.sugCallback.onSuggestionSelected(editor.sug
			        .accessCurrentSelection());
			return false;
		}
		return super.stopNewFormulaCreation(newValue0, latex, callback);
	}

	public boolean getAutoComplete() {
		return true;
	}

	/**
	 * Note that this method should set the text of the MathQuillGGB-editing box
	 * in MathQuillGGB text() format, not latex()... that's why we should have a
	 * mapping from text() format formulas to latex() format formulas, and keep
	 * it in the historyMap class, which should be filled the same time when
	 * addToHistory is filled!
	 */
	public void setText(String s) {
		editor.setText(s, false);
	}

	public List<String> resetCompletions() {
		return editor.resetCompletions();
	}











	public List<String> getCompletions() {
		return editor.getCompletions();
	}

	@Override
	public void setFocus(boolean b) {
		//App.printStacktrace("FOCUS" + b);
		DrawEquationWeb.focusEquationMathQuillGGB(seMayLatex, b);

		// if (b)
		// geogebra.html5.main.DrawEquationWeb.scrollCursorIntoView(this,
		// seMayLatex);
		// put to focus handler

		// just allow onFocus/onBlur handlers for new formula creation mode now,
		// a.k.a. this class, but later we may want to add this feature to
		// RadioButtonTreeItem, or editing mode (for existing formulas)
		if (b) {
			onFocus(null);
		} else {
			onBlur(null);
		}
	}

	public void showPopup(boolean show) {
		if (this.xButton == null) {
			return;
		}
		Element showSymbolElement = this.xButton.getElement();
		// App.debug("AF focused" + show);
		if (showSymbolElement != null
		        && "true"
		                .equals(showSymbolElement.getAttribute("data-visible"))) {
			if (show) {
				showSymbolElement.addClassName("shown");
			} else {
				if (!"true".equals(showSymbolElement
				        .getAttribute("data-persist"))) {
					showSymbolElement.removeClassName("shown");
				}
			}
		}
		if (this.pButton == null) {
			return;
		}
		showSymbolElement = this.pButton.getElement();
		if (showSymbolElement != null
				&& "true"
						.equals(showSymbolElement.getAttribute("data-visible"))) {
			if (show) {
				showSymbolElement.addClassName("shown");
			} else {
				if (!"true".equals(showSymbolElement
						.getAttribute("data-persist"))) {
					showSymbolElement.removeClassName("shown");
				}
			}
		}
	}

	public void onFocus(FocusEvent event) {
		updateGUIfocus(event == null ? this : event.getSource());

		app.getSelectionManager().clearSelectedGeos();

		// this.focused = true; // hasFocus is not needed, AFAIK
	}

	void updateGUIfocus(Object source) {
		((AlgebraViewW) av).setActiveTreeItem(null);
		if (((AlgebraViewW) av).isNodeTableEmpty()) {
			((AlgebraDockPanelW) app.getGuiManager().getLayout()
			        .getDockManager().getPanel(App.VIEW_ALGEBRA))
			        .showStyleBarPanel(false);
		} else {
			// the else branch is important since no onBlur on blur!
			((AlgebraDockPanelW) app.getGuiManager().getLayout()
			        .getDockManager().getPanel(App.VIEW_ALGEBRA))
			        .showStyleBarPanel(true);
		}



		// this is a static method, and the same which is needed here too,
		// so why duplicate the same thing in another copy?
		// this will call the showPopup method, by the way
		AutoCompleteTextFieldW.showSymbolButtonIfExists(source, true);

	}

	@SuppressWarnings("unused")
	public void onBlur(BlurEvent event) {
		// This method is practically never called, but if it will be,
		// decision shall be made whether it's Okay to make the XButton
		// invisible or not (TODO)
		if (true)
			return;

		((AlgebraDockPanelW) app.getGuiManager().getLayout().getDockManager()
		        .getPanel(App.VIEW_ALGEBRA)).showStyleBarPanel(true);

		Object source = this;
		if (event != null)
			source = event.getSource();

		// this is a static method, and the same which is needed here too,
		// so why duplicate the same thing in another copy?
		// this will call the showPopup method, by the way
		AutoCompleteTextFieldW.showSymbolButtonIfExists(source, false);

		// this.focused = false; // hasFocus is not needed, AFAIK
	}

	public ArrayList<String> getHistory() {
		return editor.getHistory();
	}

	/**
	 * Add a history popup list and an embedded popup button. See
	 * AlgebraInputBar
	 */
	public void addHistoryPopup(boolean isDownPopup) {

		if (historyPopup == null)
			historyPopup = new HistoryPopupW(this);

		historyPopup.setDownPopup(isDownPopup);

		ClickHandler al = new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				// AGString cmd = event.;
				// AGif (cmd.equals(1 + BorderButton.cmdSuffix)) {
				// TODO: should up/down orientation be tied to InputBar?
				// show popup
				historyPopup.showPopup();

			}
		};
		setBorderButton(1, BasicIcons.createUpDownTriangleIcon(false, true), al);
		this.setBorderButtonVisible(1, false);
	}

	private void setBorderButtonVisible(int i, boolean b) {
		App.debug("setBorderVisible() implementation needed"); // TODO
		                                                       // Auto-generated
	}

	private void setBorderButton(int i, ImageData createUpDownTriangleIcon,
	        ClickHandler al) {
		App.debug("setBorderButton() implementation needed"); // TODO
		                                                      // Auto-generated
	}





	@Override
	public void addToHistory(String str, String latex) {
		editor.addToHistory(str, latex);
	}

	@Override
	public boolean isSuggesting() {
		return editor.sug.isSuggestionListShowing();
	}

	@Override
	public void requestFocus() {
		// TODO Auto-generated method stub

	}


	@Override
	public void updatePosition(ScrollableSuggestionDisplay sug) {
		sug.setPositionRelativeTo(ihtml);
	}


}

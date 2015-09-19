package org.easyrpg.player.button_mapping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;

public class ButtonMappingModel {
	public static final int NUM_VERSION = 1;
	public static final String TAG_VERSION = "version", TAG_PRESETS = "presets", DEFAULT_NAME = "default",
			TAG_ID = "id", TAG_NAME = "name", TAG_BUTTONS = "buttons", TAG_KEYCODE = "keycode", TAG_X = "x",
			TAG_Y = "y", TAG_SIZE = "size";
	public static final String FILE_NAME = "button_mapping.txt";

	public LinkedList<Layout> layout_list;

	public ButtonMappingModel() {
		layout_list = new LinkedList<Layout>();
	}

	public void add(Layout p) {
		layout_list.add(p);
	}

	public JSONObject serialize() {
		JSONObject o = new JSONObject();

		try {
			JSONArray presets = new JSONArray();
			for (Layout p : this.layout_list) {
				presets.put(p.serialize());
			}

			o.put(TAG_VERSION, NUM_VERSION);
			o.put(TAG_PRESETS, presets);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return o;
	}

	public static ButtonMappingModel getDefaultButtonMappingModel(Context context) {
		ButtonMappingModel m = new ButtonMappingModel();
		m.add(Layout.getDefaultPreset(context));
		return m;
	}

	public static ButtonMappingModel readButtonMappingFile(Context context, String path) {
		ButtonMappingModel m = new ButtonMappingModel();
		JSONObject jso;

		// Read the file
		String file = new String(), st;
		BufferedReader bf;
		try {
			bf = new BufferedReader(new FileReader(new File(path)));
			while ((st = bf.readLine()) != null) {
				file += st;
			}
			bf.close();

			// Parse the JSON
			jso = new JSONObject(file);
		} catch (Exception e) {
			m.add(Layout.getDefaultPreset(context));
			return m;
		}

		// Presets' extraction
		JSONArray layout_array;
		try {
			layout_array = jso.getJSONArray("presets");
			JSONObject p;
			for (int i = 0; i < layout_array.length(); i++) {
				p = (JSONObject) layout_array.get(i);
				m.add(Layout.deserialize(context, p));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return m;
	}

	public static ButtonMappingModel getButtonMapping(Context context) {
		String button_mapping_path = Environment.getExternalStorageDirectory().getPath() + "/easyrpg/" + FILE_NAME;
		return readButtonMappingFile(context, button_mapping_path);
	}

	public LinkedList<Layout> getLayout_list() {
		return layout_list;
	}

	public static void writeButtonMappingFile(ButtonMappingModel m) {
		String button_mapping_path = Environment.getExternalStorageDirectory().getPath() + "/easyrpg/" + FILE_NAME;
		FileWriter file = null;

		try {
			file = new FileWriter(button_mapping_path);
			JSONObject obj = m.serialize();
			file.write(obj.toString(4));
			file.flush();
			file.close();
			System.out.println("Successfully Copied JSON Object to File...");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static class Layout {
		public LinkedList<VirtualButton> button_list = new LinkedList<VirtualButton>();
		String name;
		int id;

		public Layout() {
		}

		public Layout(String name) {
			this.name = name;
		}

		public Layout(String name, int id) {
			this(name);
			this.id = id;
		}

		public void add(VirtualButton v) {
			button_list.add(v);
		}

		public JSONObject serialize() {
			JSONObject preset = new JSONObject();

			try {
				preset.put(TAG_NAME, name);
				preset.put(TAG_ID, id);

				// Circle/Buttons
				JSONArray l = new JSONArray();
				for (VirtualButton b : button_list) {
					JSONObject jso = new JSONObject();
					jso.put(TAG_KEYCODE, b.getKeyCode());
					jso.put(TAG_X, b.getPosX());
					jso.put(TAG_Y, b.getPosY());
					jso.put(TAG_SIZE, b.getSize());

					l.put(jso);
				}
				preset.put(TAG_BUTTONS, l);
			} catch (JSONException e) {
				Log.e("JSONException", e.getLocalizedMessage());
			}

			return preset;
		}

		public static Layout deserialize(Context context, JSONObject jso) {
			Layout preset = new Layout();
			try {
				String name = jso.getString(TAG_NAME);
				int id = jso.getInt(TAG_ID);

				JSONArray button_list = new JSONArray();
				for (int i = 0; i < button_list.length(); i++) {
					JSONObject b = (JSONObject) button_list.get(i);
					int keyCode = b.getInt(TAG_KEYCODE), size = b.getInt(TAG_SIZE);
					double posX = b.getDouble(TAG_X), posY = b.getDouble(TAG_Y);
					if (keyCode == VirtualButton.DPAD) {
						preset.add(new VirtualCross(context, posX, posY, size));
					} else {
						preset.add(new VirtualButton(context, keyCode, posX, posY, size));
					}
				}

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return preset;
		}

		/** Return the default button mapping preset : one cross, two buttons */
		public static Layout getDefaultPreset(Context context) {
			Layout b = new Layout(DEFAULT_NAME);

			b.add(new VirtualCross(context, 0.0, 0.5, 100));
			b.add(new VirtualButton(context, VirtualButton.ENTER, 0.80, 0.7, 100));
			b.add(new VirtualButton(context, VirtualButton.CANCEL, 0.90, 0.6, 100));

			return b;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}

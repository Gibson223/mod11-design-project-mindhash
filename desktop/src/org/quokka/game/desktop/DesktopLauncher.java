package org.quokka.game.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.mygdx.game.desktop.Space;
import org.quokka.game.LidarGame;
import org.quokka.kotlin.PointCloudVisualizer;


public class DesktopLauncher {

	public static void main(String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.height = 600;
		config.width = 800;
		new LwjglApplication(new Space(), config);
	}
}

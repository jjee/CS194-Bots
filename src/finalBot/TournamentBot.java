package finalBot;

import java.util.Properties;

import org.bwapi.proxy.ProxyBot;
import org.bwapi.proxy.ProxyBotFactory;
import org.bwapi.proxy.ProxyServer;
import org.bwapi.proxy.model.Game;

import edu.berkeley.nlp.starcraft.overmind.Overmind;


public class TournamentBot {
	public static void main(String[] args) {
		ProxyBotFactory factory = new ProxyBotFactory() {

			@Override
			public ProxyBot getBot(Game g) {
				return new Overmind(new FinalBot(), new Properties());
			}
			
		};
		String heartbeatFilename = args.length > 1 ? args[1] : null;
		new ProxyServer(factory,ProxyServer.extractPort(args.length> 0 ? args[0] : null), heartbeatFilename).run();

	}

}
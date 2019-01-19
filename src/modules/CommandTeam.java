package modules;

import simple_soccer_lib.AbstractTeam;
import simple_soccer_lib.PlayerCommander;

public class CommandTeam extends AbstractTeam{
	public CommandTeam(String name) {
		super("Time"+name, 7, true);
	}

	@Override
	protected void launchPlayer(int ag, PlayerCommander commander) {
		System.out.println("Player lançado");
		CommandPlayer p = new CommandPlayer(commander);
		p.start();
	}
	
	
}

package modules;

import java.net.UnknownHostException;

public class Main {
	
	/*
	tamanho do campo
	x = 52,5 ate -52,5
	y = 34 ate -34
	*/
	
	public static void main(String [] args) throws UnknownHostException{
			//AbstractTeam a = new AbstractTeam("timeB", 7, "nao ligo", 69, true);
			CommandTeam timeA = new CommandTeam("A");
			CommandTeam timeB = new CommandTeam("B");
			timeA.launchTeamAndServer();
			//timeA.launchTeam();
			
			timeB.launchTeam();
			
	}
}

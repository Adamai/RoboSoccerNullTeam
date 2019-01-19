package modules;

public enum PlayerState {
	NULL					(0),
	STANDBY				(1),
	RECEBER				(2),
	PASSAR 				(3),
	CHUTARGOL 			(4),
	DRIBLAR 			(5),
	PEGARBOLA 			(6),
	OFENSIVA 			(7),
	DEFENSIVA 			(8),
	SUPORTE 			(9),
	CORNER_KICK_LEFT 		(10),
	CORNER_KICK_RIGHT 		(11),
	GOAL_KICK_LEFT 			(12),
	GOAL_KICK_RIGHT 		(13),
	AFTER_GOAL_LEFT 		(14),
	AFTER_GOAL_RIGHT 		(15),
	DROP_BALL 				(16),
	OFFSIDE_LEFT 			(17),
	OFFSIDE_RIGHT 			(18),
	MAX 					(19),
	BACK_PASS_LEFT			(32),	// Falta: recuo para o goleiro do time left
	BACK_PASS_RIGHT			(33),	// Falta: recuo para o goleiro do time right
	FREE_KICK_FAULT_LEFT  	(34),
	FREE_KICK_FAULT_RIGHT 	(35),
	INDIRECT_FREE_KICK_LEFT (38),	// Cobrança indireta para o time left (2 toques)
	INDIRECT_FREE_KICK_RIGHT(39);	// Cobrança indireta para o time right (2 toques)
	
	private int value;

	PlayerState(int value){
		this.value = value;
	}
	
	public static PlayerState valueOf(int playerStateValue){
		PlayerState[] values = PlayerState.values();
		for (PlayerState v : values) {
			if(playerStateValue == v.value){
				return v;
			}
		}
		System.err.println("New match state found: "+playerStateValue);
		return NULL;
	}
}

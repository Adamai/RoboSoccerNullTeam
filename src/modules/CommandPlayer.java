package modules;

import java.awt.Rectangle;
import java.util.ArrayList;

import simple_soccer_lib.PlayerCommander;
import simple_soccer_lib.perception.FieldPerception;
import simple_soccer_lib.perception.MatchPerception;
import simple_soccer_lib.perception.PlayerPerception;
import simple_soccer_lib.utils.EFieldSide;
import simple_soccer_lib.utils.Vector2D;

public class CommandPlayer extends Thread {
	private int LOOP_INTERVAL = 100; // 0,1 segundos

	private PlayerCommander commander;
	private PlayerPerception selfPerc;
	private FieldPerception fieldPerc;
	private MatchPerception matchPerc;

	public CommandPlayer(PlayerCommander player) {
		commander = player;
	}

	@Override
	public void run() {
		System.out.println(">> Executando...");
		long nextIteration = System.currentTimeMillis() + LOOP_INTERVAL;
		updatePerceptions();

		switch (selfPerc.getUniformNumber()) {

		case 1:
			acaoGoleiro(nextIteration);
			break;

		case 2:
			acaoArmador(nextIteration, -2); // cima
			break;
		case 3:
			acaoArmador(nextIteration, -1); // cima
			break;
		case 4:
			acaoArmador(nextIteration, 2); // baixo
			break;
		case 5:
			acaoArmador(nextIteration, 1); // baixo
			break;
		case 6:
			acaoAtacante(nextIteration, 1);
			break;
		case 7:
			acaoAtacante(nextIteration, -1);
			break;

		default:
			break;
		}
	}

	private void updatePerceptions() {
		PlayerPerception newSelf = commander.perceiveSelfBlocking();
		FieldPerception newField = commander.perceiveFieldBlocking();
		MatchPerception newMatch = commander.perceiveMatchBlocking();

		if (newSelf != null)
			this.selfPerc = newSelf;
		if (newField != null)
			this.fieldPerc = newField;
		if (newMatch != null)
			this.matchPerc = newMatch;
	}

	private void turnToPoint(Vector2D point) {
		Vector2D newDirection = point.sub(selfPerc.getPosition());
		commander.doTurnToDirectionBlocking(newDirection);
	}

	private void dash(Vector2D point) {
		if (selfPerc.getPosition().distanceTo(point) <= 1)
			return;

		if (!isAlignToPoint(point, 10))
			turnToPoint(point);
		commander.doDashBlocking(95);
	}

	private void kickToPoint(Vector2D point, double intensity) {
		Vector2D newDirection = point.sub(selfPerc.getPosition());
		double angle = newDirection.angleFrom(selfPerc.getDirection());
		if (angle > 90 || angle < -90) {
			commander.doTurnToDirectionBlocking(newDirection);
			angle = 0;
		}
		commander.doKickBlocking(intensity, angle);
	}

	private boolean isAlignToPoint(Vector2D point, double margin) {
		double angle = point.sub(selfPerc.getPosition()).angleFrom(selfPerc.getDirection());
		return angle < margin && angle > margin * (-1);
	}

	private boolean isPointsAreClose(Vector2D reference, Vector2D point, double margin) {
		return reference.distanceTo(point) <= margin;
	}

	/**
	 * Retorna a percepcao do jogador mais proximo do ponto do primeiro parametro.
	 * 
	 * 
	 */
	private PlayerPerception getClosestPlayerPoint(Vector2D point, EFieldSide side, double margin) {
		ArrayList<PlayerPerception> lp = fieldPerc.getTeamPlayers(side);
		PlayerPerception np = null;
		if (lp != null && !lp.isEmpty()) {
			double dist, temp;
			dist = lp.get(0).getPosition().distanceTo(point);
			np = lp.get(0);
			if (isPointsAreClose(np.getPosition(), point, margin))
				return np;
			for (PlayerPerception p : lp) {
				if (p.getPosition() == null)
					break;
				if (isPointsAreClose(p.getPosition(), point, margin)) {
					return p;
				}
				temp = p.getPosition().distanceTo(point);
				if (temp < dist) {
					dist = temp;
					np = p;
				}
			}

		}
		return np;
	}

	private void acaoGoleiro(long nextIteration) {
		PlayerState goleiroState = PlayerState.STANDBY;
		double xInit = -48, yInit = 0, ballX = 0, ballY = 0;
		EFieldSide side = selfPerc.getSide();
		EFieldSide side2 = EFieldSide.invert(side);
		Vector2D initPos = new Vector2D(xInit * side.value(), yInit * side.value());
		Vector2D ballPos;
		Vector2D goalPos = new Vector2D(50 * side.value(), 0); // gol do oponente
		Vector2D closest;
		Vector2D posicaoChute = new Vector2D(36 * side.value(), 0);
		double temp = 999;
		Rectangle halfSide = side == EFieldSide.LEFT ? new Rectangle(-52, -34, 26, 68) : new Rectangle(26, -34, 26, 68);
		// USAR HALF SIDE PRA GOLEIRO SEGUIR Y DA BOLA
		Rectangle area = side == EFieldSide.LEFT ? new Rectangle(-52, -20, 16, 40) : new Rectangle(36, -20, 16, 40);
		while (true) {
			updatePerceptions();
			ballPos = fieldPerc.getBall().getPosition();

			switch (matchPerc.getState()) {
			case BEFORE_KICK_OFF: // POSICIONA
				commander.doMoveBlocking(xInit, yInit);
				break;
			case GOAL_KICK_LEFT:
				if (side == EFieldSide.LEFT) {
					dash(new Vector2D(ballPos.getX() - 1, ballPos.getY()));
					if (isPointsAreClose(selfPerc.getPosition(), new Vector2D(ballPos.getX() - 1, ballPos.getY()),
							0.5)) {
						turnToPoint(ballPos);
						kickToPoint(goalPos, 100);
					}
				} else {
					dash(initPos);
				}
				break;
			case GOAL_KICK_RIGHT:
				if (side == EFieldSide.RIGHT) {
					dash(new Vector2D(ballPos.getX() + 1, ballPos.getY()));
					if (isPointsAreClose(selfPerc.getPosition(), new Vector2D(ballPos.getX() + 1, ballPos.getY()),
							0.5)) {
						turnToPoint(ballPos);
						kickToPoint(goalPos, 100);
					}
				} else {
					dash(initPos);
				}
				break;
			case AFTER_GOAL_LEFT:
				dash(initPos);
				break;
			case AFTER_GOAL_RIGHT:
				dash(initPos);
				break;
			case KICK_OFF_LEFT:
				dash(new Vector2D(xInit * side.value(), (ballPos.getY() / 21) * (3 + yInit) * (side.value())));
				break;
			case FREE_KICK_LEFT:
				dash(new Vector2D(xInit * side.value(), (ballPos.getY() / 21) * (3 + yInit) * (side.value())));
				break;
			case FREE_KICK_RIGHT:
				dash(new Vector2D(xInit * side.value(), (ballPos.getY() / 21) * (3 + yInit) * (side.value())));
				break;
			case KICK_IN_RIGHT:
				dash(new Vector2D(xInit * side.value(), (ballPos.getY() / 21) * (3 + yInit) * (side.value())));
				break;
			case KICK_IN_LEFT:
				dash(new Vector2D(xInit * side.value(), (ballPos.getY() / 21) * (3 + yInit) * (side.value())));
				break;
			case PLAY_ON:
				ballX = fieldPerc.getBall().getPosition().getX();
				ballY = fieldPerc.getBall().getPosition().getY();

				switch (goleiroState) {
				case STANDBY:
					if (halfSide.contains(ballX, ballY)) { // PREPARAR PARA DEFENDER
						goleiroState = PlayerState.DEFENSIVA;
						break;
					}
					if (!isPointsAreClose(selfPerc.getPosition(), initPos, 0.6)) {
						dash(initPos); // IR PARA POSICAO INICIAL
					} else {
						turnToPoint(ballPos); // VIRAR PRA BOLA
					}

					break;
				case DEFENSIVA:
					if (area.contains(ballX, ballY)) { // PREPARAR PARA PEGAR A BOLA
						goleiroState = PlayerState.PEGARBOLA;
						break;
					} else if (!halfSide.contains(ballX, ballY)) { // VOLTAR A STANDBY
						goleiroState = PlayerState.STANDBY;
						break;
					}
					double Ygoal = ballY;
					if (ballPos.getY() > 7) { // ACOMPANHAR Y DA BOLA EM RELAÇÃO AO GOL
						Ygoal = 3.4;
					} else if (ballPos.getY() < -7) {
						Ygoal = -3.4;
					}
					dash(new Vector2D(selfPerc.getPosition().getX(), Ygoal));
					if (isPointsAreClose(selfPerc.getPosition(), new Vector2D(selfPerc.getPosition().getX(), Ygoal),
							0.1)) {
						turnToPoint(ballPos); // VIRAR PRA BOLA
					}
					break;
				case PEGARBOLA:
					if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) { // PREPARANDO PARA PASSAR
						commander.doCatchBlocking(0);
						goleiroState = PlayerState.PASSAR;
						break;
					} else if (!area.contains(ballX, ballY)) { // VOLTANDO A DEFENSIVA
						goleiroState = PlayerState.DEFENSIVA;
						break;
					}
					dash(ballPos);
					break;
				case PASSAR:
					if (!isPointsAreClose(selfPerc.getPosition(), ballPos, 2)) { // VOLTANDO A DEFENSIVA
						goleiroState = PlayerState.DEFENSIVA;
						break;
					}
					commander.doCatchBlocking(0);
					// 6 ta marcado? Se não, chuta pra ele
					for (int i = 1; i < 8; i++) { // verifica todos os jogadores do outro time
						if (fieldPerc.getTeamPlayer(side2, i).getPosition()
								.distanceTo(fieldPerc.getTeamPlayer(side, 6).getPosition()) < temp) {
							closest = fieldPerc.getTeamPlayer(side2, i).getPosition(); // guarda posicao do jogador mais
																						// proximo
							temp = fieldPerc.getTeamPlayer(side2, i).getPosition()
									.distanceTo(fieldPerc.getTeamPlayer(side, 6).getPosition()); // guarda distancia pro
																									// jog mais proximo
						}
					}
					if (temp > 6) { // se a distancia for maior que (6), chuta pro 6
						kickToPoint(fieldPerc.getTeamPlayer(side, 6).getPosition(), 70); // CHUTAR
						temp = 999;
					} else { // 6 tava marcado. Chuta pro 7?
						for (int i = 1; i < 8; i++) { // verifica todos os jogadores do outro time
							if (fieldPerc.getTeamPlayer(side2, i).getPosition()
									.distanceTo(fieldPerc.getTeamPlayer(side, 7).getPosition()) < temp) {
								closest = fieldPerc.getTeamPlayer(side2, i).getPosition(); // guarda posicao do jogador
																							// mais proximo
								temp = fieldPerc.getTeamPlayer(side2, i).getPosition()
										.distanceTo(fieldPerc.getTeamPlayer(side, 7).getPosition()); // guarda distancia
																										// pro jog mais
																										// proximo
							}
						}
						if (temp > 6) { // se a distancia for maior que (6), chuta pro 7
							kickToPoint(fieldPerc.getTeamPlayer(side, 7).getPosition(), 70); // CHUTAR
							temp = 999;
						} else { // chuta pra frente
							kickToPoint(goalPos, 100);
						}
					}
					break;

				}

				break;

			// OUTROS ESTADOS

			default:
				break;
			}
		}
	}

	private void acaoArmador(long nextIteration, int pos) {
		PlayerState armadorState = PlayerState.DEFENSIVA;
		double xInit = -30, yInit = 10 * pos;
		EFieldSide side = selfPerc.getSide();
		Vector2D initPos = new Vector2D(xInit * side.value(), yInit * side.value());
		Vector2D defPos = new Vector2D((-36 * side.value()), 5 * pos);
		Vector2D ballPos, vTemp;
		PlayerPerception pTemp;
		Rectangle halfSide;
		if(side == EFieldSide.LEFT) {
			halfSide = new Rectangle(-52, -34, 26, 68);
		} else {
			halfSide = new Rectangle(26, -34, 26, 68);
		}
		while (true) {
			updatePerceptions();
			ballPos = fieldPerc.getBall().getPosition();
			switch (matchPerc.getState()) {

			case BEFORE_KICK_OFF:
				commander.doMoveBlocking(xInit, yInit);
				break;
			case GOAL_KICK_LEFT:
				dash(initPos);
				break;
			case GOAL_KICK_RIGHT:
				dash(initPos);
				break;
			case AFTER_GOAL_LEFT:
				dash(initPos);
				break;
			case AFTER_GOAL_RIGHT:
				dash(initPos);
				break;
			case KICK_OFF_LEFT:
				dash(initPos);
				break;
			case KICK_OFF_RIGHT:
				dash(initPos);
				break;
			case FREE_KICK_LEFT:
				if (selfPerc.getSide() == EFieldSide.LEFT) {
					dash(initPos);
				} else {
					dash(defPos);
				}
				break;
			case FREE_KICK_RIGHT:
				if (selfPerc.getSide() == EFieldSide.RIGHT) {
					dash(initPos);
				} else {
					dash(defPos);
				}
				break;
			case PLAY_ON:
				pTemp = getClosestPlayerPoint(ballPos, side, 10);
				switch (armadorState) {
				case DEFENSIVA:
					if (pTemp != null && pTemp.getUniformNumber() == selfPerc.getUniformNumber()) { // PEGAR BOLA SE
																									// PROXIMO
						armadorState = PlayerState.PEGARBOLA;
						break;
					} else if (!halfSide.contains(ballPos.getX(), ballPos.getY())) {
						armadorState = PlayerState.OFENSIVA;		//entrando aqui
						break;
					}
					else if (!isPointsAreClose(selfPerc.getPosition(), defPos, 3)) {
						dash(defPos);
					} else {
						turnToPoint(ballPos);
					}

					break;
				case OFENSIVA:
					if (pTemp != null && pTemp.getUniformNumber() == selfPerc.getUniformNumber()) { // PEGAR BOLA SE
																									// PROXIMO
						armadorState = PlayerState.PEGARBOLA;
						break;
					}
					if (side == EFieldSide.LEFT) { // se estamos do lado esquerdo
						if (ballPos.getX() < 0) {
							// defender se a bola esta longe de todos os jogadores
							armadorState = PlayerState.DEFENSIVA;
							break;
						}
					} else if (side == EFieldSide.RIGHT) { // se estamos do lado direito
						if (ballPos.getX() < 0) {
							// defender se a bola esta longe de todos os jogadores
							armadorState = PlayerState.DEFENSIVA;
							break;
						}
					}
					if (selfPerc.getUniformNumber() == 2) {
						dash(new Vector2D(((xInit * side.value()) + ballPos.getX()) / 3, yInit * side.value()));
					} else if (selfPerc.getUniformNumber() == 3) {
						dash(new Vector2D(((xInit * side.value()) + ballPos.getX()) / 2,
								((yInit * side.value()) + ballPos.getY()) / 2));
					} else if (selfPerc.getUniformNumber() == 4) {
						dash(new Vector2D(((xInit * side.value()) + ballPos.getX()) / 2, yInit * side.value()));
					} else {
						dash(new Vector2D(((xInit * side.value()) + ballPos.getX()) / 3,
								((yInit * side.value()) + ballPos.getY()) / 5));
					}

					break;
				case PEGARBOLA:
					if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) { // PREPARANDO PARA PASSAR
						armadorState = PlayerState.PASSAR;
						break;
					} else if (pTemp.getUniformNumber() != selfPerc.getUniformNumber()) {
						armadorState = PlayerState.DEFENSIVA;
						break;
					}
					// pega a bola
					dash(ballPos);
					break;
				case PASSAR:
					if (selfPerc.getUniformNumber() == 7) {
						// toca para o jogador 7
						vTemp = fieldPerc.getTeamPlayer(side, 3).getPosition();
					} else if (selfPerc.getUniformNumber() == 3) {
						// toca para o jogador 7
						vTemp = fieldPerc.getTeamPlayer(side, 7).getPosition();
					} else if (selfPerc.getUniformNumber() == 4) {
						// toca para o jogador 6
						vTemp = fieldPerc.getTeamPlayer(side, 6).getPosition();
					} else if (selfPerc.getUniformNumber() == 5) {
						// toca para o jogador 6
						vTemp = fieldPerc.getTeamPlayer(side, 6).getPosition();
					} else {
						// nunca entra?
						vTemp = fieldPerc.getTeamPlayer(side, 7).getPosition();
					}

					double intensity;
					// CONCERTAR ISSO AQ ?
					if (isPointsAreClose(selfPerc.getPosition(), vTemp, 30)) {
						Vector2D vTempF = vTemp.sub(selfPerc.getPosition());
						intensity = (vTempF.magnitude() * 100) / 40;
					} else {
						intensity = 5;
					}
					kickToPoint(vTemp, intensity);

					armadorState = PlayerState.DEFENSIVA;
					break;
				}

//				
//				if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
//					
//				}
//				// SE NENHUM ARMADOR DO TIME ESTA COM A POSSE ENTRA NESSE ELSE
//				else {
//					pTemp = getClosestPlayerPoint(ballPos, side, 3);
//
//					if (!isPointsAreClose(pTemp.getPosition(), ballPos, 15) && pTemp.getPosition() != defPos) {
//						// defender se a bola esta longe de todos os jogadores
//						
//						dash(defPos);
//						
//					} else if (pTemp != null && pTemp.getUniformNumber() == selfPerc.getUniformNumber()) {
//						// pega a bola
//						dash(ballPos);
//					}
//					// se a bola está do outro lado do campo e na posse do time
//					else if (ballPos.getX() > 0 && (isPointsAreClose(pTemp.getPosition(), ballPos, 5))
//							&& selfPerc.getSide() == EFieldSide.LEFT) {
//						if (selfPerc.getUniformNumber() == 2) {
//							dash(new Vector2D( ((xInit * side.value()) + ballPos.getX())/3 , yInit * side.value()));
//						} else if (selfPerc.getUniformNumber() == 3) {
//							dash(new Vector2D( ((xInit * side.value()) + ballPos.getX())/2 , ((yInit * side.value()) + ballPos.getY())/2));
//						} else if (selfPerc.getUniformNumber() == 4) {
//							dash(new Vector2D( ((xInit * side.value()) + ballPos.getX())/2 , yInit * side.value()));
//						} else {
//							dash(new Vector2D( ((xInit * side.value()) + ballPos.getX())/3 , ((yInit * side.value()) + ballPos.getY())/5));
//						}
//					} else if (!isPointsAreClose(selfPerc.getPosition(), initPos, 3)) {
//						// recua
//						dash(initPos);
//					} else {
//						// olha para a bola
//						turnToPoint(ballPos);
//					}
//				}
				break;

			/* Todos os estados da partida */

			default:
				break;
			}
		}
	}

	private void acaoAtacante(long nextIteration, int pos) {
		PlayerState atacanteState = PlayerState.STANDBY;
		double xInit = -15, yInit = 5 * pos;
		EFieldSide side = selfPerc.getSide();
		Vector2D initPos = new Vector2D(xInit * side.value(), yInit);
		Vector2D goalPos = new Vector2D(50 * side.value(), 0);
		Vector2D ballPos;
		Vector2D closest;
		PlayerPerception pTemp;
		double marcDist = 6;
		double temp2 = 999;
		double blockDist = 5;
		EFieldSide side2 = EFieldSide.invert(side);

		while (true) {
			updatePerceptions();
			ballPos = fieldPerc.getBall().getPosition();
			free:
			switch (matchPerc.getState()) {

			case BEFORE_KICK_OFF:
				if (selfPerc.getSide() == EFieldSide.LEFT) {
					if (selfPerc.getUniformNumber() == 6) {
						commander.doMoveBlocking(0, 0.5);
					} else {
						commander.doMoveBlocking(0, -9);
					}
				} else {
					if (selfPerc.getUniformNumber() == 6) {
						commander.doMoveBlocking(xInit, yInit);
					} else {
						commander.doMoveBlocking(xInit, yInit);
					}
				}
				break;
			case GOAL_KICK_LEFT:
				// dash(initPos);
				break;
			case GOAL_KICK_RIGHT:
				// dash(initPos);
				break;
			case AFTER_GOAL_LEFT:
				dash(initPos);
				break;
			case AFTER_GOAL_RIGHT:
				dash(initPos);
				break;
			case KICK_OFF_LEFT:
				if (selfPerc.getSide() == EFieldSide.LEFT) {
					if (selfPerc.getUniformNumber() == 6) {
						dash(new Vector2D(0, -0.5));
						if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
							// turnToPoint(fieldPerc.getTeamPlayer(side, 6).getPosition());
							if (isPointsAreClose(fieldPerc.getTeamPlayer(side, 7).getPosition(), new Vector2D(0, -9),
									0.3)) {
								Vector2D posPassar = new Vector2D(
										fieldPerc.getTeamPlayer(side, 7).getPosition().getX() + 1,
										fieldPerc.getTeamPlayer(side, 7).getPosition().getY());
								MensageiroPlayer.getInstance().postMensagem("Passando", 7, posPassar.getX(),
										posPassar.getY());
								kickToPoint(posPassar, 50);
//								if (side == EFieldSide.LEFT)
//									System.out.println("6 passando pro 7");
								atacanteState = PlayerState.SUPORTE;
							}
						}
					} else {
						dash(new Vector2D(0, -9));
						if (isPointsAreClose(selfPerc.getPosition(), new Vector2D(0, -9), 0)) {
							turnToPoint(ballPos);
						}
					}
				} else { // não é o time que vai chutar
					dash(initPos);
					if (isPointsAreClose(selfPerc.getPosition(), initPos, 0))
						turnToPoint(ballPos);
				}
				break;
			case KICK_OFF_RIGHT:
				if (selfPerc.getSide() == EFieldSide.RIGHT) {
					if (selfPerc.getUniformNumber() == 6) {
						dash(new Vector2D(0, -0.5));
						if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
							// turnToPoint(fieldPerc.getTeamPlayer(side, 6).getPosition());
							if (isPointsAreClose(fieldPerc.getTeamPlayer(side, 7).getPosition(), new Vector2D(0, -9),
									0.3)) {
								Vector2D posPassar = new Vector2D(
										fieldPerc.getTeamPlayer(side, 7).getPosition().getX() - 1,
										fieldPerc.getTeamPlayer(side, 7).getPosition().getY());
								MensageiroPlayer.getInstance().postMensagem("Passando", 7, posPassar.getX(),
										posPassar.getY());
								kickToPoint(posPassar, 50);
//								if (side == EFieldSide.LEFT)
//									System.out.println("6 passando pro 7");
								atacanteState = PlayerState.SUPORTE;
							}
						}
					} else {
						dash(new Vector2D(0, -9));
						if (isPointsAreClose(selfPerc.getPosition(), new Vector2D(0, -9), 0)) {
							turnToPoint(ballPos);
						}
					}
				} else { // não é o time que vai chutar
					dash(initPos);
					if (isPointsAreClose(selfPerc.getPosition(), initPos, 0))
						turnToPoint(ballPos);
				}

				break;
			case FREE_KICK_LEFT:
				if (selfPerc.getSide() == EFieldSide.LEFT) {
					if (selfPerc.getUniformNumber() == 7) {
						dash(ballPos);
						if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
							kickToPoint(goalPos, 100);
						}

					} else {
						dash(new Vector2D(ballPos.getX(), 0));
					}
				}
				break;
			case FREE_KICK_RIGHT:
				if (selfPerc.getSide() == EFieldSide.RIGHT) {
					if (selfPerc.getUniformNumber() == 7) {
						dash(ballPos);
						if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
							kickToPoint(goalPos, 100);
						}

					} else {
						dash(new Vector2D(ballPos.getX(), 0));
					}
				}
				break;
			case KICK_IN_RIGHT:
				if (selfPerc.getSide() == EFieldSide.RIGHT) {
					if (selfPerc.getUniformNumber() == 7) {
						dash(ballPos);
						if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
							kickToPoint(goalPos, 100);
						}
					}
				} else {
					dash(initPos);
				}

				break;
			case KICK_IN_LEFT:
				if (selfPerc.getSide() == EFieldSide.LEFT) {
					if (selfPerc.getUniformNumber() == 7) {
						dash(ballPos);
						if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
							kickToPoint(goalPos, 100);
						}
					}
				} else {
					dash(initPos);
				}

				break;
			case PLAY_ON:
				pTemp = getClosestPlayerPoint(ballPos, side, 10);
				if (MensageiroPlayer.getInstance().getDestino() == selfPerc.getUniformNumber()) {
					if (MensageiroPlayer.getInstance().getMensagem().equals("Passando")) {
						atacanteState = PlayerState.RECEBER;
//						if (side == EFieldSide.LEFT)
//							System.out.println(Integer.toString(selfPerc.getUniformNumber()) + ": vou receber");
					}
				}
				switch (atacanteState) {
				case STANDBY:
					if (pTemp == selfPerc) {
						//if (side == EFieldSide.LEFT)
						//	System.out.println(Integer.toString(selfPerc.getUniformNumber()) + ": vou atrás da bola");
						atacanteState = PlayerState.PEGARBOLA;
						break;
					} else if (pTemp != selfPerc) {
						if (pTemp.getUniformNumber() == 7) { // se n sou o jogador mais proximo e ele é o 7 (sou o 6)
							//if (side == EFieldSide.LEFT)
							//	System.out.println(Integer.toString(selfPerc.getUniformNumber()) + ": vou ajudar o 7");
							atacanteState = PlayerState.SUPORTE;
							break;
						} else if (pTemp.getUniformNumber() == 6) { // se n sou o jogador mais proximo e ele é o 6 (sou
																	// o 7)
							//if (side == EFieldSide.LEFT)
							//	System.out.println(Integer.toString(selfPerc.getUniformNumber()) + ": vou ajudar o 6");
							atacanteState = PlayerState.SUPORTE;
							break;
						}
					}
					if(!(pTemp.getUniformNumber() > 0 && pTemp.getUniformNumber() < 6)) {
						dash(ballPos);  //ninguem do time ta perto da bola
					}
					
					break;
				case PEGARBOLA:
					if (pTemp != selfPerc) {
						atacanteState = PlayerState.STANDBY;
						break;
					} else if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
						atacanteState = PlayerState.OFENSIVA;
						break;
					}
					dash(ballPos);
					break;
				case OFENSIVA:
					if (isPointsAreClose(selfPerc.getPosition(), ballPos, 0.5)) {
						if (isPointsAreClose(ballPos, goalPos, 30)) {
							atacanteState = PlayerState.CHUTARGOL;
							break free;
						} else if (selfPerc.getUniformNumber() == 6) { // verifica se 6 ta marcado se ele tiver com a bola
//							if(side == EFieldSide.LEFT) {
//								System.out.println("é pra driblar ou passar?");
//							}
							for (int i = 1; i < 8; i++) {
								if (fieldPerc.getTeamPlayer(side2, i).getPosition()
										.distanceTo(selfPerc.getPosition()) < marcDist) {// se entrar aqui é pq ta marcado
//									if(side == EFieldSide.LEFT) {
//										System.out.println("ta marcado");
//									}
									for (int j = 1; j < 8; j++) { // verificando se o 7 ta BLOQUEADO também
										if (fieldPerc.getTeamPlayer(side2, j).getPosition().distanceTo(
												fieldPerc.getTeamPlayer(side, 7).getPosition()) < blockDist) {
											atacanteState = PlayerState.DRIBLAR; // tem alguem BLOQUEANDO o 7 (nao entra?)
//											if(side == EFieldSide.LEFT) {
//												System.out.println("driblando");
//											}
											break free;
										} else if (j == 7) {
											atacanteState = PlayerState.PASSAR; // ngm marcando o 7 (nao entra?)
//											if(side == EFieldSide.LEFT) {
//												System.out.println("passando pro 7");
//											}
											break free;
										}
									}
								} else if(i == 7) { // se ele n ta marcado, conduz
									// conduz para o gol
									kickToPoint(goalPos, 18);
									atacanteState = PlayerState.PEGARBOLA;
									break free;
								}
							}
						} else { // se 7 ta marcado se ele tiver com a bola
//							if(side == EFieldSide.LEFT) {
//								System.out.println("é pra driblar ou passar 2?");
//							}
							for (int i = 1; i < 8; i++) {
								if (fieldPerc.getTeamPlayer(side2, i).getPosition()
										.distanceTo(selfPerc.getPosition()) < marcDist) {// ta marcado
//									if(side == EFieldSide.LEFT) {
//										System.out.println("ta marcado");
//									}
									for (int j = 1; j < 8; j++) { // verificando se o 6 ta BLOQUEADO
										if (fieldPerc.getTeamPlayer(side2, j).getPosition().distanceTo(
												fieldPerc.getTeamPlayer(side, 6).getPosition()) < blockDist) {
											atacanteState = PlayerState.DRIBLAR; // 6 ta BLOQUEADO (não entra?)
//											if(side == EFieldSide.LEFT) {
//												System.out.println("driblando");
//											}
											break free;
										} else if (i == 7) { // 6 nao ta BLOQUEADO (nao entra?)
											atacanteState = PlayerState.PASSAR;
//											if(side == EFieldSide.LEFT) {
//												System.out.println("passando pro 6");
//											}
											break free;
										}
									}
								} else if(i == 7) { // se ele n ta marcado, conduz
									// conduz para o gol
									kickToPoint(goalPos, 18);
									atacanteState = PlayerState.PEGARBOLA;
									break free;
								}
							}
						}
						
					} else if (pTemp != selfPerc && pTemp.getUniformNumber() > 5 && pTemp.getUniformNumber() < 8) {
//						if(EFieldSide.LEFT == selfPerc.getSide())
//							System.out.println(Integer.toString(selfPerc.getUniformNumber())+": virando suporte. pTemp é "+pTemp.getUniformNumber());
						atacanteState = PlayerState.SUPORTE;
						break free;
					} else if (matchPerc.getTime() > 11) {
						dash(ballPos);
					}

					break;
				case RECEBER:
					Vector2D posicao = new Vector2D(MensageiroPlayer.getInstance().getPosX(),
							MensageiroPlayer.getInstance().getPosY());
					if (isPointsAreClose(selfPerc.getPosition(), posicao, 0.1)) { // cheguei na posicao destino?
						if (!isPointsAreClose(selfPerc.getPosition(), ballPos, 10)) { // a bola foi pra longe de algum
																						// jeito. Standby
							atacanteState = PlayerState.STANDBY;
							break;
						} else if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) { // a bola chegou em mim na
																							// posicao
							atacanteState = PlayerState.OFENSIVA;
							break;
						} else { // olhar para a bola
							turnToPoint(ballPos);
						}
					} else if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) { // nao cheguei ainda mas a bola
																						// chegou em mim
						atacanteState = PlayerState.OFENSIVA;
						break;
					} else { // não cheguei ainda. Vou para la
						dash(posicao);
					}

					break;
				case PASSAR:
					if (selfPerc.getUniformNumber() == 6) {
						Vector2D posPassar = new Vector2D(fieldPerc.getTeamPlayer(side, 7).getPosition().getX() + 3,
								fieldPerc.getTeamPlayer(side, 7).getPosition().getY());
						MensageiroPlayer.getInstance().postMensagem("Passando", 7, posPassar.getX(), posPassar.getY());
						kickToPoint(posPassar, 30);
//						if (side == EFieldSide.LEFT)
//							System.out.println("6 passando pro 7");
					} else if (selfPerc.getUniformNumber() == 7) {
						Vector2D posPassar = new Vector2D(fieldPerc.getTeamPlayer(side, 6).getPosition().getX() + 3,
								fieldPerc.getTeamPlayer(side, 6).getPosition().getY());
						MensageiroPlayer.getInstance().postMensagem("Passando", 6, posPassar.getX(), posPassar.getY());
						kickToPoint(posPassar, 30);
//						if (side == EFieldSide.LEFT)
//							System.out.println("7 passando pro 6");
					}
					atacanteState = PlayerState.SUPORTE;
					break;
				case CHUTARGOL:
					// chuta para o gol
					kickToPoint(goalPos, 100);
					atacanteState = PlayerState.STANDBY;
					break;
				case DRIBLAR:
					for (int i = 1; i < 8; i++) {
						if (isPointsAreClose(fieldPerc.getTeamPlayer(side2, i).getPosition(), selfPerc.getPosition(),
								marcDist)) {
							if (fieldPerc.getTeamPlayer(side2, i).getPosition().getY() > selfPerc.getPosition()
									.getY()) { // se ele vem de baixo
								kickToPoint(new Vector2D(goalPos.getX(), selfPerc.getPosition().getY() - 9), 18);
							} else { // se ele vem de cima
								kickToPoint(new Vector2D(goalPos.getX(), selfPerc.getPosition().getY() + 9), 18);
							}
						}
					}
					atacanteState = PlayerState.OFENSIVA;
					break;
				case SUPORTE:
					if ((pTemp == null || pTemp == selfPerc) && matchPerc.getTime() > 20) {
						atacanteState = PlayerState.OFENSIVA;
						break;
					}
					if (selfPerc.getUniformNumber() == 6) {
						dash(new Vector2D(fieldPerc.getTeamPlayer(side, 7).getPosition().getX(),
								fieldPerc.getTeamPlayer(side, 7).getPosition().getY() + 10));
					} else {
						dash(new Vector2D(fieldPerc.getTeamPlayer(side, 6).getPosition().getX(),
								fieldPerc.getTeamPlayer(side, 6).getPosition().getX() - 10));
					}
					break;
				}
				break;

			/* Todos os estados da partida */

			default:
				break;
			}
		}
	}
}

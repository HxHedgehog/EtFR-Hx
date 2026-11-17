package ganymedes01.etfuturum.core.utils;

/**
 * Duck interface mixed onto EntityPlayer: 26.2's Player.attackStrengthTicker / itemSwapTicker pair
 * (Player.tick L262-269). The left-click gate compares the attack charge against
 * ItemSpear.getAttackDurationTicks() (26.2 MINIMUM_ATTACK_CHARGE = 1.0 against the ATTACK_SPEED
 * attribute 1/attackDuration - 4, which makes the delay equal the stab animation length); the
 * first-person equip raise reads the swap charge (ItemInHandRenderer L565).
 */
public interface SpearChargeTicker {

	/**
	 * 26.2 cannotAttackWithItem: (ticker + tolerance) / delay clamped to [0, 1]; the attack is
	 * allowed once this reaches 1.0. tolerance mirrors 26.2's latency allowance (0 client, 5 server).
	 */
	float etfu$spearAttackCharge(int tolerance, int durationTicks);

	/**
	 * 26.2 getItemSwapScale(1.0): (swapTicker + 1) / delay clamped to [0, 1]; the equip raise
	 * targets this value cubed.
	 */
	float etfu$spearSwapCharge(int durationTicks);

	/**
	 * 26.2 resetOnlyAttackStrengthTicker: attacks clear just the attack charge, so the equip raise
	 * is not replayed.
	 */
	void etfu$resetSpearAttackCharge();

	/**
	 * 26.2 resetAttackStrengthTicker: a main-hand item change clears both tickers, which is what
	 * makes swapping mid-swing unable to skip the recovery.
	 */
	void etfu$resetSpearChargeTickers();
}

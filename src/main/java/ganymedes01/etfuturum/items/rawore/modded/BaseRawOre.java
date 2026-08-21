package ganymedes01.etfuturum.items.rawore.modded;

import ganymedes01.etfuturum.items.BaseItem;
import ganymedes01.etfuturum.lib.Reference;

public class BaseRawOre extends BaseItem {
	private final String subfolder;

	public BaseRawOre(String subfolder, String name) {
		this.subfolder = subfolder;
		setNames("raw_" + name);
	}

	@Override
	public String getTextureSubfolder() {
		// Raw ores are placed flat in the item atlas (textures/items/raw_*.png),
		// alongside the vanilla raw_* set, instead of a per-mod sub-folder.
		// See getTextureSubfolder()/setTextureName() in BaseItem.
		return "";
	}

	@Override
	public String getTextureDomain() {
		return Reference.MOD_ID;
	}

	@Override
	public String getNameDomain() {
		// The owning mod still labels the item's registry/name domain (e.g.
		// "etfuturum.simpleores.raw_adamantium") so this is separate from the
		// flat texture path above.
		return super.getNameDomain() + (subfolder.isEmpty() ? "" : (super.getNameDomain().isEmpty() ? "" : ".") + subfolder);
	}
}

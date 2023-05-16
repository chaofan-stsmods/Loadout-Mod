package loadout.screens;

import basemod.abstracts.AbstractCardModifier;
import basemod.helpers.CardModifierManager;
import basemod.helpers.TooltipInfo;
import basemod.patches.whatmod.WhatMod;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.colorless.Madness;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.*;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import loadout.LoadoutMod;
import loadout.relics.AbstractCustomScreenRelic;
import loadout.relics.OrbBox;
import loadout.savables.Favorites;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class CardModSelectScreen extends AbstractSelectScreen<CardModSelectScreen.CardModButton>{
    public static class CardModButton {
        public String id;
        public String name;
        public String modID;
        public Hitbox hb;
        public float x;
        public float y;
        public AbstractCardModifier instance;
        public ArrayList<PowerTip> tips;
        public int amount;
        public CardModButton(AbstractCardModifier instance) {
            this.instance = instance;
            this.id = instance.identifier(null);
            if(id == null) this.id = instance.getClass().getName();

            this.modID = WhatMod.findModID(instance.getClass());
            if (this.modID == null) this.modID = "Slay the Spire";

            this.x = 0;
            this.y = 0;
            this.amount = 0;

            this.hb = new Hitbox(200.0f * Settings.scale,75.0f * Settings.yScale);
            this.tips = new ArrayList<>();

            List<TooltipInfo> tooltips = this.instance.additionalTooltips(null);
            if(tooltips != null && !tooltips.isEmpty()) {

                for (int i = 0; i < tooltips.size(); i++) {
                    TooltipInfo first = tooltips.get(i);
                    if (i == 0) {
                        this.name = first.title;
                    }
                    this.tips.add(first.toPowerTip());
                }

//                TooltipInfo first = tooltips.remove(0);
//
//
//                for(TooltipInfo ti: tooltips) {
//                    this.tips.add(ti.toPowerTip());
//                }
            }

            if(name == null) this.name = instance.getClass().getSimpleName();
            this.tips.add(0,new PowerTip(this.name, ""));
        }
        public void update() {
            this.hb.update();
        }

        public void render(SpriteBatch sb) {
            if(this.hb != null) {
                this.hb.render(sb);
                float a = (amount != 0 || this.hb.hovered) ? 1.0f : 0.7f;

                if (this.hb.hovered) {
                    sb.setBlendFunction(770, 1);
                    sb.setColor(new Color(1.0F, 1.0F, 1.0F, 0.3F));
                    sb.draw(ImageMaster.CHAR_OPT_HIGHLIGHT, x+40.0F,y-64.0F, 64.0F, 64.0F, 300.0f, 100.0f, Settings.scale, Settings.scale, 0.0F, 0, 0, 256, 256, false, false);
                    FontHelper.renderSmartText(sb,FontHelper.buttonLabelFont,this.name,x+150.0f / 2,y + 20.0f,200.0f,25.0f,Settings.GOLD_COLOR);
                    sb.setBlendFunction(770, 771);

                    TipHelper.queuePowerTips(InputHelper.mX + 60.0F * Settings.scale, InputHelper.mY + 180.0F * Settings.scale, this.tips);
                } else {
                    FontHelper.renderSmartText(sb,FontHelper.buttonLabelFont,this.name,x+150.0f / 2,y + 20.0f,200.0f,25.0f,Settings.CREAM_COLOR);
                }
            }

        }
        public void applyMod(AbstractCard card) {
            CardModifierManager.addModifier(card, instance.makeCopy());
        }

        public void removeMod(AbstractCard card) {
            CardModifierManager.removeModifiersById(card, id, true);
        }
    }

    public AbstractCard currentCard = new Madness();

    public ArrayList<AbstractCard> cards;

    private static final Comparator<CardModButton> BY_NAME = Comparator.comparing(c -> c.name);
    private static final Comparator<CardModButton> BY_ID = Comparator.comparing(c -> c.id);

    private static final Comparator<CardModButton> BY_MOD = Comparator.comparing(c -> c.modID);


    public CardModSelectScreen() {
        super(null);
        this.itemsPerLine = 5;
        this.sortHeader = new CardModSortHeader(this);
        this.defaultSortType = SortType.MOD;
    }

    @Override
    public void open() {
        super.open();
    }

    public void open(AbstractCard card, ArrayList<AbstractCard> cards) {
        show = true;
        doneSelecting = false;

        this.currentCard = card;
        this.cards = cards;

        this.confirmButton.isDisabled = false;
        this.confirmButton.show();
        callOnOpen();

        sortOnOpen();
        calculateScrollBounds();

        this.selectedItems.clear();
    }

    @Override
    public void close() {
        this.show = false;
        InputHelper.justReleasedClickLeft = false;
        this.confirmButton.hide();
        this.confirmButton.isDisabled = true;
    }

    @Override
    protected boolean testFilters(CardModButton item) {
        return true;
    }

    @Override
    public void sort(boolean isAscending) {
        switch (currentSortType){
            case NAME:
                sortAlphabetically(isAscending);
                break;
            case MOD:
                sortByMod(isAscending);
                break;
        }
    }
    public void sortAlphabetically(boolean isAscending){
        if (isAscending) {
            this.currentSortOrder = SortOrder.ASCENDING;
            if (shouldSortById()) this.items.sort(BY_ID);
            else this.items.sort(BY_NAME);
        } else {
            this.currentSortOrder = SortOrder.DESCENDING;
            if (shouldSortById()) this.items.sort(BY_ID.reversed());
            else this.items.sort(BY_NAME.reversed());
        }
        this.currentSortType = SortType.NAME;
        scrolledUsingBar(0.0F);
    }
    public void sortByMod(boolean isAscending){
        if (isAscending) {
            this.currentSortOrder = SortOrder.ASCENDING;
            this.items.sort(BY_MOD.thenComparing(BY_ID));
        } else {
            this.currentSortOrder = SortOrder.DESCENDING;
            this.items.sort(BY_MOD.reversed().thenComparing(BY_ID));
        }
        this.currentSortType = SortType.MOD;
        scrolledUsingBar(0.0F);
    }

    @Override
    protected void callOnOpen() {
        targetY = scrollLowerBound;
        scrollY = Settings.HEIGHT - 400.0f * Settings.scale;

        if(this.itemsClone == null || this.itemsClone.isEmpty()) {
            //first time
            this.itemsClone = new ArrayList<>();
            for (Class<?extends AbstractCardModifier> cardMod : LoadoutMod.cardModMap.values()) {
                try {
                    itemsClone.add(new CardModButton(cardMod.getDeclaredConstructor(new Class[] {}).newInstance()));
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                    LoadoutMod.logger.info("Error creating button for " + cardMod.getName());
                    e.printStackTrace();
                    continue;
                } catch (NoClassDefFoundError noClassError) {
                    LoadoutMod.logger.info("ERROR THROWN! NO CLASS DEF FOUND FOR " + cardMod.getName());
                    continue;
                }
            }


            this.items = new ArrayList<>(itemsClone);
        }
    }

    @Override
    protected void updateItemClickLogic() {
        if(hoveredItem != null) {
            if (InputHelper.justClickedLeft || CInputActionSet.select.isJustPressed()) {
                clickStartedItem = hoveredItem;
                //logger.info("Pressed Left");
            }
            if (InputHelper.justReleasedClickLeft || CInputActionSet.select.isJustPressed())
            {
                CInputActionSet.select.unpress();
                if (hoveredItem == clickStartedItem)
                {
                    if(isFaving) {
                        String pID = hoveredItem.id;
                        //TODO Add to fav

                        if(filterFavorites)
                            updateFilters();

                        try {
                            LoadoutMod.favorites.save();
                        } catch (IOException e) {
                            LoadoutMod.logger.info("Failed to save favorites");
                        }
                    } else {
                        clickStartedItem.amount += selectMult;
                        //TODO modify amount

                        clickStartedItem.applyMod(currentCard);
                    }

                    clickStartedItem = null;

                    if (doneSelecting()) {
                        close();
                    }
                }
            }

            if (InputHelper.justClickedRight || CInputActionSet.select.isJustPressed()) {
                clickStartedItem = hoveredItem;

            }
            if (InputHelper.justReleasedClickRight || CInputActionSet.select.isJustPressed())
            {
                CInputActionSet.select.unpress();
                if (hoveredItem == clickStartedItem)
                {
                    clickStartedItem.amount -= selectMult;
                    //TODO modify amount

                    clickStartedItem.removeMod(currentCard);

                    clickStartedItem = null;
                }
            }

        } else {
            clickStartedItem = null;
        }

    }

    @Override
    protected void updateList(ArrayList<CardModButton> list) {
        this.currentCard.target_x = 200.0f * Settings.scale;
        this.currentCard.target_y = Settings.HEIGHT / 2f;
        this.currentCard.update();

        if (this.confirmButton.hb.hovered) return;

        for (CardModButton cmb : list)
        {
            cmb.update();
            cmb.hb.move(cmb.x  + 150.0f, cmb.y);

            if (cmb.hb.hovered)
            {
                hoveredItem = cmb;
            }
        }
    }

    @Override
    protected void renderList(SpriteBatch sb, ArrayList<CardModButton> list) {

        this.currentCard.render(sb);

        row += 1;
        col = 0;
        float curX;
        float curY;
        GOLD_OUTLINE_COLOR.a = 0.3f;
        char prevFirst = '\0';
        String prevMod = "";
        scrollTitleCount = 0;

        for (Iterator<CardModButton> it = list.iterator(); it.hasNext(); ) {
            CardModButton cardModButton = it.next();
            if(LoadoutMod.enableCategory&&this.currentSortType!=null) {
                if (currentSortType == SortType.NAME) {

                    char pFirst = (shouldSortById() || cardModButton.name== null || cardModButton.name.length() == 0) ?   cardModButton.id.toUpperCase().charAt(0) : cardModButton.name.toUpperCase().charAt(0);

                    if (pFirst != prevFirst) {
                        row++;
                        scrollTitleCount++;

                        //if new type, render new texts
                        prevFirst = pFirst;

                        String msg = "Undefined:";
                        String desc = "Error";
                        if (prevFirst != '\0') {
                            msg = String.valueOf(prevFirst).toUpperCase() + ":";
                            desc = "";
                        }

                        FontHelper.renderSmartText(sb, FontHelper.buttonLabelFont, msg, START_X - 50.0F * Settings.scale, this.scrollY + 4.0F * Settings.scale - SPACE * this.row, 99999.0F, 0.0F, Settings.GOLD_COLOR);
                        if (LoadoutMod.enableDesc) FontHelper.renderSmartText(sb, FontHelper.cardDescFont_N, desc, START_X - 50.0F * Settings.scale +

                                FontHelper.getSmartWidth(FontHelper.buttonLabelFont, msg, 99999.0F, 0.0F), this.scrollY - 0.0F * Settings.scale - SPACE * this.row, 99999.0F, 0.0F, Settings.CREAM_COLOR);
                        row++;
                        col = 0;
                    }
                } else if (currentSortType == SortType.MOD) {
                    String pMod = cardModButton.modID;
                    if (pMod == null) pMod = "Slay the Spire";
                    if (!pMod.equals(prevMod)) {
                        row++;
                        scrollTitleCount++;

                        //if new type, render new texts
                        prevMod = pMod;

                        String msg = "Undefined:";
                        String desc = "Error";
                        if (prevMod != null) {
                            msg = prevMod + ":";
                            desc = "";
                        }
                        //remove other lines
                        if (desc.contains("NL")) {
                            desc = desc.split(" NL ")[0];
                        } else if (desc.equals("StsOrigPlaceholder")) {
                            desc = RelicSelectScreen.TEXT[6];
                        }

                        FontHelper.renderSmartText(sb, FontHelper.buttonLabelFont, msg, START_X - 50.0F * Settings.scale, this.scrollY + 4.0F * Settings.scale - SPACE * this.row, 99999.0F, 0.0F, Settings.GOLD_COLOR);
                        if (LoadoutMod.enableDesc) FontHelper.renderSmartText(sb, FontHelper.cardDescFont_N, desc, START_X - 50.0F * Settings.scale +

                                FontHelper.getSmartWidth(FontHelper.buttonLabelFont, msg, 99999.0F, 0.0F), this.scrollY - 0.0F * Settings.scale - SPACE * this.row, 99999.0F, 20.0F, Settings.CREAM_COLOR);
                        row++;
                        col = 0;
                    }
                }
            }
            if (col == this.itemsPerLine) {
                col = 0;
                row += 1;
            }
            curX = (START_X + SPACE_X * col);
            curY = (scrollY - SPACE * row);

            cardModButton.x = curX;
            cardModButton.y = curY;

            if(filterAll && Favorites.favoritePowers.contains(cardModButton.id)) {

                sb.setColor(GOLD_BACKGROUND);
                sb.draw(ImageMaster.CHAR_OPT_HIGHLIGHT,curX - (float)128 / 2.0F, curY - (float)128 / 2.0F, (float)128, (float)128);
            }

            cardModButton.render(sb);

            col += 1;
        }
        calculateScrollBounds();
    }


}

using UnityEngine;

public class CodMobileHudAndTouch : MonoBehaviour
{
    public MobileFpsController player;

    private int moveTouchId = -1;
    private int lookTouchId = -1;
    private Vector2 moveOrigin;
    private Vector2 lookLastPos;

    private Rect fireBtnRect;
    private Rect adsBtnRect;
    private Rect reloadBtnRect;
    private Rect slideBtnRect;

    void OnGUI()
    {
        if (player == null) return;

        float w = Screen.width;
        float h = Screen.height;

        HandleMobileTouches(w, h);

        // Viseur central + Hitmarker
        float cx = w * 0.5f;
        float cy = h * 0.5f;
        GUI.color = player.lastHitMarker ? Color.red : Color.white;
        GUI.DrawTexture(new Rect(cx - 10f, cy - 1.5f, 20f, 3f), Texture2D.whiteTexture);
        GUI.DrawTexture(new Rect(cx - 1.5f, cy - 10f, 3f, 20f), Texture2D.whiteTexture);

        // HUD Santé & Munitions (style COD Mobile)
        GUI.color = Color.white;
        GUI.Box(new Rect(20f, 20f, 230f, 34f), $"HP: {Mathf.CeilToInt(player.currentHealth)}  |  KILLS: {player.kills}");
        string ammoLabel = player.isReloading
            ? $"RELOAD {Mathf.RoundToInt(player.reloadProgress * 100f)}%"
            : $"{player.currentAmmo} / 30";
        GUI.Box(new Rect(w - 220f, 20f, 200f, 34f), $"AMMO: {ammoLabel}  |  PTS: {player.score}");

        // Boutons tactiles à droite
        float btnW = 110f;
        float btnH = 54f;
        fireBtnRect = new Rect(w - btnW - 24f, h - btnH - 24f, btnW, btnH);
        adsBtnRect = new Rect(w - btnW * 2f - 36f, h - btnH - 24f, btnW, btnH);
        reloadBtnRect = new Rect(w - btnW - 24f, h - btnH * 2f - 36f, btnW, btnH);
        slideBtnRect = new Rect(w - btnW * 2f - 36f, h - btnH * 2f - 36f, btnW, btnH);

        if (GUI.RepeatButton(fireBtnRect, "FIRE"))
        {
            player.isFiringHeld = true;
        }
        else if (!Input.GetMouseButton(0))
        {
            player.isFiringHeld = false;
        }

        if (GUI.RepeatButton(adsBtnRect, player.IsAiming() ? "ADS [ON]" : "ADS"))
        {
            player.isAdsHeld = true;
        }
        else if (!Input.GetMouseButton(1))
        {
            player.isAdsHeld = false;
        }

        if (GUI.Button(reloadBtnRect, "RELOAD"))
        {
            player.StartReload();
        }

        if (GUI.Button(slideBtnRect, "SLIDE"))
        {
            player.TriggerSlide();
        }
    }

    void HandleMobileTouches(float w, float h)
    {
        if (Input.touchCount == 0)
        {
            moveTouchId = -1;
            lookTouchId = -1;
            player.moveInput = Vector2.zero;
            player.lookInput = Vector2.zero;
            return;
        }

        Vector2 currentLook = Vector2.zero;
        for (int i = 0; i < Input.touchCount; i++)
        {
            Touch t = Input.GetTouch(i);
            Vector2 guiPos = new Vector2(t.position.x, h - t.position.y);

            if (fireBtnRect.Contains(guiPos) || adsBtnRect.Contains(guiPos) ||
                reloadBtnRect.Contains(guiPos) || slideBtnRect.Contains(guiPos))
            {
                continue;
            }

            if (t.position.x < w * 0.45f)
            {
                if (t.phase == TouchPhase.Began && moveTouchId == -1)
                {
                    moveTouchId = t.fingerId;
                    moveOrigin = t.position;
                }
                else if (t.fingerId == moveTouchId)
                {
                    if (t.phase == TouchPhase.Moved || t.phase == TouchPhase.Stationary)
                    {
                        Vector2 delta = (t.position - moveOrigin) / 90f;
                        player.moveInput = Vector2.ClampMagnitude(delta, 1f);
                    }
                    else if (t.phase == TouchPhase.Ended || t.phase == TouchPhase.Canceled)
                    {
                        moveTouchId = -1;
                        player.moveInput = Vector2.zero;
                    }
                }
            }
            else
            {
                if (t.phase == TouchPhase.Began && lookTouchId == -1)
                {
                    lookTouchId = t.fingerId;
                    lookLastPos = t.position;
                }
                else if (t.fingerId == lookTouchId)
                {
                    if (t.phase == TouchPhase.Moved)
                    {
                        Vector2 d = t.position - lookLastPos;
                        lookLastPos = t.position;
                        currentLook += new Vector2(d.x * 0.22f, d.y * 0.22f);
                    }
                    else if (t.phase == TouchPhase.Ended || t.phase == TouchPhase.Canceled)
                    {
                        lookTouchId = -1;
                    }
                }
            }
        }
        player.lookInput = currentLook;
    }
}

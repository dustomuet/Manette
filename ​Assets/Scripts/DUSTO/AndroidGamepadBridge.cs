using UnityEngine;

public class AndroidGamepadBridge : MonoBehaviour
{
    public MobileFpsController fpsController;

    [Header("Sensibilité Manette Bluetooth / OTG")]
    public float stickLookSpeed = 2.8f;
    public float deadzone = 0.14f;
    public bool invertY = false;

    void Update()
    {
        if (fpsController == null) return;

        // Sticks analogiques (compatibilité standard Android / Unity Input)
        float lx = ApplyDeadzone(Input.GetAxisRaw("Horizontal"));
        float ly = ApplyDeadzone(Input.GetAxisRaw("Vertical"));

        float rx = ReadRightStickX();
        float ry = ReadRightStickY();

        if (Mathf.Abs(lx) > 0.01f || Mathf.Abs(ly) > 0.01f)
        {
            fpsController.moveInput = new Vector2(lx, ly);
        }

        if (Mathf.Abs(rx) > 0.01f || Mathf.Abs(ry) > 0.01f)
        {
            float ySign = invertY ? 1f : -1f;
            fpsController.lookInput = new Vector2(rx * stickLookSpeed, ry * stickLookSpeed * ySign);
        }

        // Gâchettes et boutons manette (Xbox / PS / générique Android)
        float rTrigger = ReadRightTrigger();
        float lTrigger = ReadLeftTrigger();

        bool padFire = rTrigger > 0.45f || Input.GetKey(KeyCode.JoystickButton5); // R1 / RB
        bool padAds = lTrigger > 0.45f || Input.GetKey(KeyCode.JoystickButton4);  // L1 / LB

        if (padFire) fpsController.isFiringHeld = true;
        if (padAds) fpsController.isAdsHeld = true;

        // Boutons d'action : X / Carré = Reload, B / Rond = Slide, A / Croix = Jump
        if (Input.GetKeyDown(KeyCode.JoystickButton2) || Input.GetKeyDown(KeyCode.JoystickButton3))
        {
            fpsController.StartReload();
        }
        if (Input.GetKeyDown(KeyCode.JoystickButton1))
        {
            fpsController.TriggerSlide();
        }
    }

    float ApplyDeadzone(float value)
    {
        if (Mathf.Abs(value) < deadzone) return 0f;
        float sign = Mathf.Sign(value);
        return sign * ((Mathf.Abs(value) - deadzone) / (1f - deadzone));
    }

    float ReadRightStickX()
    {
        float x = TryGetAxis("RightStickX");
        if (Mathf.Abs(x) < 0.01f) x = TryGetAxis("Mouse X") * 0.25f;
        return ApplyDeadzone(x);
    }

    float ReadRightStickY()
    {
        float y = TryGetAxis("RightStickY");
        if (Mathf.Abs(y) < 0.01f) y = TryGetAxis("Mouse Y") * 0.25f;
        return ApplyDeadzone(y);
    }

    float ReadRightTrigger()
    {
        float t = TryGetAxis("RightTrigger");
        if (t < 0f) t = (t + 1f) * 0.5f;
        return Mathf.Clamp01(t);
    }

    float ReadLeftTrigger()
    {
        float t = TryGetAxis("LeftTrigger");
        if (t < 0f) t = (t + 1f) * 0.5f;
        return Mathf.Clamp01(t);
    }

    float TryGetAxis(string axisName)
    {
        try
        {
            return Input.GetAxisRaw(axisName);
        }
        catch
        {
            return 0f;
        }
    }
}

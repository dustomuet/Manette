using UnityEngine;
using UnityEngine.SceneManagement;

[RequireComponent(typeof(CharacterController))]
public class MobileFpsController : MonoBehaviour
{
    [Header("Caméra & Visée (Style COD Mobile)")]
    public Camera playerCamera;
    public Transform weaponHolder;
    public float mouseSensitivity = 2.2f;
    public float normalFov = 75f;
    public float adsFov = 46f;
    public float adsSpeed = 14f;
    public float standingCamHeight = 1.68f;
    public float crouchCamHeight = 1.05f;
    public float slideCamHeight = 0.82f;

    [Header("Déplacement, Sprint & Glissade")]
    public float walkSpeed = 6.2f;
    public float sprintSpeed = 9.8f;
    public float slideSpeed = 14.5f;
    public float slideDuration = 0.55f;
    public float gravity = -24f;
    public float jumpForce = 7.5f;

    [Header("Arme, Munitions & Recul")]
    public float fireRate = 0.11f;
    public float range = 130f;
    public float damage = 32f;
    public int magazineSize = 30;
    public float reloadTime = 1.25f;
    public float recoilKick = 0.085f;
    public float recoilRecovery = 14f;

    [Header("Santé & Progression")]
    public float maxHealth = 100f;
    public float currentHealth { get; private set; }
    public int kills { get; private set; }
    public int score { get; private set; }
    public int currentAmmo { get; private set; }
    public bool isReloading { get; private set; }
    public float reloadProgress { get; private set; }
    public bool lastHitMarker { get; private set; }

    private CharacterController controller;
    private Vector3 velocity;
    private Vector3 defaultWeaponPos;
    private Vector3 adsWeaponPos = new Vector3(0f, -0.14f, 0.36f);
    private Vector3 currentRecoilOffset;

    private float pitch = 0f;
    private float nextFireTime = 0f;
    private float slideTimer = 0f;
    private Vector3 slideDirection;
    private float reloadTimer = 0f;
    private float hitMarkerTimer = 0f;

    [HideInInspector] public Vector2 moveInput;
    [HideInInspector] public Vector2 lookInput;
    [HideInInspector] public bool isAdsHeld;
    [HideInInspector] public bool isFiringHeld;
    [HideInInspector] public bool isSprintingHeld;

    void Start()
    {
        controller = GetComponent<CharacterController>();
        if (playerCamera == null) playerCamera = Camera.main;
        if (weaponHolder != null) defaultWeaponPos = weaponHolder.localPosition;
        currentHealth = maxHealth;
        currentAmmo = magazineSize;
    }

    void Update()
    {
        if (hitMarkerTimer > 0f)
        {
            hitMarkerTimer -= Time.deltaTime;
            if (hitMarkerTimer <= 0f) lastHitMarker = false;
        }

        HandleLook();
        HandleMovementAndSlide();
        HandleAdsAndWeapon();
        HandleReloadAndFire();
    }

    void HandleLook()
    {
        float mx = Input.GetAxis("Mouse X") * mouseSensitivity + lookInput.x;
        float my = Input.GetAxis("Mouse Y") * mouseSensitivity + lookInput.y;

        float adsMultiplier = IsAiming() ? 0.62f : 1f;
        transform.Rotate(Vector3.up * mx * adsMultiplier);

        pitch -= my * adsMultiplier;
        pitch = Mathf.Clamp(pitch, -80f, 80f);
        if (playerCamera != null)
        {
            playerCamera.transform.localRotation = Quaternion.Euler(pitch, 0f, 0f);
        }
    }

    void HandleMovementAndSlide()
    {
        float h = Mathf.Abs(moveInput.x) > 0.05f ? moveInput.x : Input.GetAxis("Horizontal");
        float v = Mathf.Abs(moveInput.y) > 0.05f ? moveInput.y : Input.GetAxis("Vertical");
        Vector3 inputDir = (transform.right * h + transform.forward * v).normalized;

        bool wantsSprint = (isSprintingHeld || Input.GetKey(KeyCode.LeftShift)) && v > 0.2f && !IsAiming();

        if (Input.GetKeyDown(KeyCode.C) && slideTimer <= 0f && controller.isGrounded)
        {
            TriggerSlide(inputDir.sqrMagnitude > 0.01f ? inputDir : transform.forward);
        }

        if (slideTimer > 0f)
        {
            slideTimer -= Time.deltaTime;
            controller.Move(slideDirection * slideSpeed * Time.deltaTime);
            controller.height = Mathf.Lerp(controller.height, 1.0f, Time.deltaTime * 16f);
        }
        else
        {
            controller.height = Mathf.Lerp(controller.height, 1.8f, Time.deltaTime * 12f);
            float speed = wantsSprint ? sprintSpeed : walkSpeed;
            controller.Move(inputDir * speed * Time.deltaTime);
        }

        if (controller.isGrounded && Input.GetButtonDown("Jump"))
        {
            velocity.y = jumpForce;
        }

        if (controller.isGrounded && velocity.y < 0f)
        {
            velocity.y = -2f;
        }
        velocity.y += gravity * Time.deltaTime;
        controller.Move(velocity * Time.deltaTime);
    }

    public void TriggerSlide()
    {
        if (slideTimer > 0f || !controller.isGrounded) return;
        float h = Mathf.Abs(moveInput.x) > 0.05f ? moveInput.x : Input.GetAxis("Horizontal");
        float v = Mathf.Abs(moveInput.y) > 0.05f ? moveInput.y : Input.GetAxis("Vertical");
        Vector3 dir = (transform.right * h + transform.forward * v);
        TriggerSlide(dir.sqrMagnitude > 0.01f ? dir.normalized : transform.forward);
    }

    public void TriggerSlide(Vector3 dir)
    {
        slideDirection = dir.normalized;
        slideTimer = slideDuration;
    }

    public bool IsAiming()
    {
        return isAdsHeld || Input.GetMouseButton(1);
    }

    void HandleAdsAndWeapon()
    {
        bool aiming = IsAiming();
        if (playerCamera != null)
        {
            float targetFov = aiming ? adsFov : normalFov;
            playerCamera.fieldOfView = Mathf.Lerp(playerCamera.fieldOfView, targetFov, Time.deltaTime * adsSpeed);

            float targetCamY = slideTimer > 0f ? slideCamHeight : standingCamHeight;
            Vector3 camPos = playerCamera.transform.localPosition;
            camPos.y = Mathf.Lerp(camPos.y, targetCamY, Time.deltaTime * 14f);
            playerCamera.transform.localPosition = camPos;
        }

        currentRecoilOffset = Vector3.Lerp(currentRecoilOffset, Vector3.zero, Time.deltaTime * recoilRecovery);

        if (weaponHolder != null)
        {
            Vector3 targetPos = aiming ? adsWeaponPos : defaultWeaponPos;
            weaponHolder.localPosition = Vector3.Lerp(
                weaponHolder.localPosition,
                targetPos + currentRecoilOffset,
                Time.deltaTime * 15f
            );
        }
    }

    void HandleReloadAndFire()
    {
        if (Input.GetKeyDown(KeyCode.R))
        {
            StartReload();
        }

        if (isReloading)
        {
            reloadTimer -= Time.deltaTime;
            reloadProgress = 1f - Mathf.Clamp01(reloadTimer / reloadTime);
            if (reloadTimer <= 0f)
            {
                currentAmmo = magazineSize;
                isReloading = false;
                reloadProgress = 0f;
            }
            return;
        }

        bool wantsFire = isFiringHeld || Input.GetMouseButton(0);
        if (wantsFire && Time.time >= nextFireTime)
        {
            if (currentAmmo <= 0)
            {
                StartReload();
                return;
            }

            nextFireTime = Time.time + fireRate;
            currentAmmo--;
            ShootRaycast();
        }
    }

    public void StartReload()
    {
        if (isReloading || currentAmmo >= magazineSize) return;
        isReloading = true;
        reloadTimer = reloadTime;
        reloadProgress = 0f;
    }

    void ShootRaycast()
    {
        currentRecoilOffset += new Vector3(0f, recoilKick * 0.35f, -recoilKick);

        if (playerCamera == null) return;
        Ray ray = playerCamera.ViewportPointToRay(new Vector3(0.5f, 0.5f, 0f));
        if (Physics.Raycast(ray, out RaycastHit hit, range))
        {
            BotTarget bot = hit.collider.GetComponentInParent<BotTarget>();
            if (bot != null)
            {
                lastHitMarker = true;
                hitMarkerTimer = 0.14f;
                bool killed = bot.TakeDamage(damage);
                if (killed)
                {
                    kills++;
                    score += 100;
                }
            }
        }
    }

    public void TakePlayerDamage(float amount)
    {
        if (currentHealth <= 0f) return;
        currentHealth = Mathf.Max(0f, currentHealth - amount);
        if (currentHealth <= 0f)
        {
            SceneManager.LoadScene(SceneManager.GetActiveScene().buildIndex);
        }
    }
}

using UnityEngine;
using UnityEngine.AI;

[RequireComponent(typeof(NavMeshAgent))]
public class BotTarget : MonoBehaviour
{
    public float maxHealth = 100f;
    public float sightRange = 32f;
    public float attackRange = 15f;
    public float fireCooldown = 0.85f;
    public float botDamage = 12f;

    private float health;
    private float nextShotTime = 0f;
    private NavMeshAgent agent;
    private MobileFpsController playerController;

    void Start()
    {
        health = maxHealth;
        agent = GetComponent<NavMeshAgent>();
        GameObject p = GameObject.FindGameObjectWithTag("Player");
        if (p != null)
        {
            playerController = p.GetComponent<MobileFpsController>();
        }
    }

    void Update()
    {
        if (playerController == null) return;

        float dist = Vector3.Distance(transform.position, playerController.transform.position);
        if (dist <= sightRange)
        {
            agent.SetDestination(playerController.transform.position);
            if (dist <= attackRange)
            {
                Vector3 lookDir = playerController.transform.position - transform.position;
                lookDir.y = 0f;
                if (lookDir.sqrMagnitude > 0.001f)
                {
                    transform.rotation = Quaternion.Slerp(
                        transform.rotation,
                        Quaternion.LookRotation(lookDir),
                        Time.deltaTime * 10f
                    );
                }

                if (Time.time >= nextShotTime)
                {
                    nextShotTime = Time.time + fireCooldown;
                    FireAtPlayer();
                }
            }
        }
    }

    void FireAtPlayer()
    {
        if (playerController == null) return;
        Vector3 origin = transform.position + Vector3.up * 1.45f;
        Vector3 targetPoint = playerController.transform.position + Vector3.up * 1.25f;
        Vector3 dir = (targetPoint - origin).normalized;

        if (Physics.Raycast(origin, dir, out RaycastHit hit, attackRange))
        {
            if (hit.collider.CompareTag("Player"))
            {
                playerController.TakePlayerDamage(botDamage);
            }
        }
    }

    public bool TakeDamage(float amount)
    {
        health -= amount;
        if (health <= 0f)
        {
            Destroy(gameObject);
            return true;
        }
        return false;
    }
}

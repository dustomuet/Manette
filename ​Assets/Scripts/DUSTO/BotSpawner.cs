using System.Collections.Generic;
using UnityEngine;

public class BotSpawner : MonoBehaviour
{
    public GameObject botPrefab;
    public Transform[] spawnPoints;
    public int maxActiveBots = 5;
    public float respawnInterval = 3.0f;

    private readonly List<GameObject> spawnedBots = new List<GameObject>();
    private float timer = 0f;

    void Start()
    {
        SpawnInitialBots();
    }

    void Update()
    {
        spawnedBots.RemoveAll(b => b == null);
        if (spawnedBots.Count >= maxActiveBots) return;

        timer += Time.deltaTime;
        if (timer >= respawnInterval)
        {
            timer = 0f;
            SpawnSingleBot();
        }
    }

    void SpawnInitialBots()
    {
        int toSpawn = Mathf.Min(maxActiveBots, spawnPoints != null ? spawnPoints.Length : 0);
        for (int i = 0; i < toSpawn; i++)
        {
            SpawnSingleBot();
        }
    }

    void SpawnSingleBot()
    {
        if (botPrefab == null || spawnPoints == null || spawnPoints.Length == 0) return;
        Transform point = spawnPoints[Random.Range(0, spawnPoints.Length)];
        GameObject bot = Instantiate(botPrefab, point.position, point.rotation);
        spawnedBots.Add(bot);
    }
}

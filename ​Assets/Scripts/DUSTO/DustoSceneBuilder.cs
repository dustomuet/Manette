#if UNITY_EDITOR
using UnityEditor;
using UnityEditor.AI;
using UnityEngine;
using UnityEngine.AI;

public static class DustoSceneBuilder
{
    [MenuItem("DUSTØ/Générer Scène FPS COD Mobile")]
    public static void BuildScene()
    {
        // Sol principal
        GameObject ground = GameObject.CreatePrimitive(PrimitiveType.Plane);
        ground.name = "Ground";
        ground.transform.localScale = new Vector3(6f, 1f, 6f);
        GameObjectUtility.SetStaticEditorFlags(ground, StaticEditorFlags.NavigationStatic);

        // Murs d'enceinte & couvertures tactiques
        CreateWall("Wall_North", new Vector3(0f, 2f, 30f), new Vector3(60f, 4f, 1f));
        CreateWall("Wall_South", new Vector3(0f, 2f, -30f), new Vector3(60f, 4f, 1f));
        CreateWall("Wall_East", new Vector3(30f, 2f, 0f), new Vector3(1f, 4f, 60f));
        CreateWall("Wall_West", new Vector3(-30f, 2f, 0f), new Vector3(1f, 4f, 60f));

        CreateWall("Cover_A", new Vector3(-6f, 1f, 6f), new Vector3(5f, 2f, 1.2f));
        CreateWall("Cover_B", new Vector3(7f, 1f, -4f), new Vector3(1.2f, 2f, 6f));
        CreateWall("Cover_C", new Vector3(0f, 1.25f, 14f), new Vector3(6f, 2.5f, 1.5f));

        // Joueur FPS
        GameObject player = new GameObject("Player");
        player.tag = "Player";
        player.transform.position = new Vector3(0f, 1f, -16f);
        player.AddComponent<CharacterController>();
        MobileFpsController fps = player.AddComponent<MobileFpsController>();

        Camera existingCam = Camera.main;
        GameObject camGo = existingCam != null ? existingCam.gameObject : new GameObject("Main Camera");
        camGo.tag = "MainCamera";
        camGo.transform.SetParent(player.transform);
        camGo.transform.localPosition = new Vector3(0f, 1.68f, 0f);
        Camera cam = camGo.GetComponent<Camera>() ?? camGo.AddComponent<Camera>();
        fps.playerCamera = cam;

        // Arme 3D simple attachée à la caméra
        GameObject weaponHolder = new GameObject("WeaponHolder");
        weaponHolder.transform.SetParent(camGo.transform);
        weaponHolder.transform.localPosition = new Vector3(0.32f, -0.26f, 0.55f);

        GameObject gunMesh = GameObject.CreatePrimitive(PrimitiveType.Cube);
        gunMesh.name = "AssaultRifleMesh";
        gunMesh.transform.SetParent(weaponHolder.transform);
        gunMesh.transform.localPosition = Vector3.zero;
        gunMesh.transform.localScale = new Vector3(0.08f, 0.11f, 0.55f);
        Object.DestroyImmediate(gunMesh.GetComponent<Collider>());
        fps.weaponHolder = weaponHolder.transform;

        // HUD tactile
        CodMobileHudAndTouch hud = player.AddComponent<CodMobileHudAndTouch>();
        hud.player = fps;

        // Prefab Bot de base
        GameObject botGo = GameObject.CreatePrimitive(PrimitiveType.Capsule);
        botGo.name = "Bot_Enemy";
        botGo.transform.position = new Vector3(0f, 1f, 18f);
        botGo.AddComponent<NavMeshAgent>();
        botGo.AddComponent<BotTarget>();

        // Points de spawn & Spawner
        Transform[] spawns = new Transform[3];
        Vector3[] positions = {
            new Vector3(-14f, 0.5f, 16f),
            new Vector3(14f, 0.5f, 16f),
            new Vector3(0f, 0.5f, 22f)
        };
        for (int i = 0; i < positions.Length; i++)
        {
            GameObject sp = new GameObject($"SpawnPoint_{i + 1}");
            sp.transform.position = positions[i];
            spawns[i] = sp.transform;
        }

        GameObject spawnerGo = new GameObject("BotSpawner");
        BotSpawner spawner = spawnerGo.AddComponent<BotSpawner>();
        spawner.botPrefab = botGo;
        spawner.spawnPoints = spawns;

        NavMeshBuilder.BuildNavMesh();
        Selection.activeGameObject = player;
    }

    static void CreateWall(string name, Vector3 pos, Vector3 scale)
    {
        GameObject wall = GameObject.CreatePrimitive(PrimitiveType.Cube);
        wall.name = name;
        wall.transform.position = pos;
        wall.transform.localScale = scale;
        GameObjectUtility.SetStaticEditorFlags(wall, StaticEditorFlags.NavigationStatic);
    }
}
#endif

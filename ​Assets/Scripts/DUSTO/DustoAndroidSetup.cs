#if UNITY_EDITOR
using UnityEditor;
using UnityEngine;

public static class DustoAndroidSetup
{
    [MenuItem("DUSTØ/Configurer Cible Android (Optimisé Mobile)")]
    public static void ConfigureAndroid()
    {
        if (EditorUserBuildSettings.activeBuildTarget != BuildTarget.Android)
        {
            EditorUserBuildSettings.SwitchActiveBuildTarget(BuildTargetGroup.Android, BuildTarget.Android);
        }

        PlayerSettings.SetApplicationIdentifier(BuildTargetGroup.Android, "com.dusto.fpsmobile");
        PlayerSettings.productName = "DUSTO OPS";
        PlayerSettings.defaultInterfaceOrientation = UIOrientation.LandscapeLeft;
        PlayerSettings.SetScriptingBackend(BuildTargetGroup.Android, ScriptingImplementation.IL2CPP);
        PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARM64;
        PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel24;

        Debug.Log("DUSTØ : Paramètres Android configurés (Paysage, IL2CPP, ARM64, API 24+).");
    }

    [MenuItem("DUSTØ/Attacher Pont Manette Android au Joueur")]
    public static void AttachGamepadBridge()
    {
        GameObject player = GameObject.FindGameObjectWithTag("Player");
        if (player == null)
        {
            Debug.LogWarning("DUSTØ : Aucun objet tagué 'Player' trouvé dans la scène.");
            return;
        }

        MobileFpsController fps = player.GetComponent<MobileFpsController>();
        AndroidGamepadBridge bridge = player.GetComponent<AndroidGamepadBridge>();
        if (bridge == null)
        {
            bridge = player.AddComponent<AndroidGamepadBridge>();
        }
        bridge.fpsController = fps;
        Selection.activeGameObject = player;
        Debug.Log("DUSTØ : AndroidGamepadBridge attaché à " + player.name);
    }
}
#endif

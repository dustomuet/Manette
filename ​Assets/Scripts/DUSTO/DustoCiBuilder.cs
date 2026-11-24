#if UNITY_EDITOR
using System.IO;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;

public static class DustoCiBuilder
{
    private const string ScenePath = "Assets/Scenes/DustoMainScene.unity";

    public static void BuildAndroidApk()
    {
        EnsurePlayerTag();
        DustoAndroidSetup.ConfigureAndroid();

        var scene = EditorSceneManager.NewScene(NewSceneSetup.DefaultGameObjects, NewSceneMode.Single);
        DustoSceneBuilder.BuildScene();
        DustoAndroidSetup.AttachGamepadBridge();

        Directory.CreateDirectory("Assets/Scenes");
        EditorSceneManager.SaveScene(scene, ScenePath);
        EditorBuildSettings.scenes = new[]
        {
            new EditorBuildSettingsScene(ScenePath, true)
        };

        string outputDir = Path.Combine(Directory.GetCurrentDirectory(), "build", "Android");
        Directory.CreateDirectory(outputDir);
        string apkPath = Path.Combine(outputDir, "DUSTO_OPS.apk");

        EditorUserBuildSettings.buildAppBundle = false;

        BuildPipeline.BuildPlayer(
            new[] { ScenePath },
            apkPath,
            BuildTarget.Android,
            BuildOptions.None
        );
    }

    private static void EnsurePlayerTag()
    {
        SerializedObject tagManager = new SerializedObject(AssetDatabase.LoadAllAssetsAtPath("ProjectSettings/TagManager.asset")[0]);
        SerializedProperty tagsProp = tagManager.FindProperty("tags");
        bool found = false;
        for (int i = 0; i < tagsProp.arraySize; i++)
        {
            if (tagsProp.GetArrayElementAtIndex(i).stringValue == "Player")
            {
                found = true;
                break;
            }
        }
        if (!found)
        {
            tagsProp.InsertArrayElementAtIndex(0);
            tagsProp.GetArrayElementAtIndex(0).stringValue = "Player";
            tagManager.ApplyModifiedProperties();
        }
    }
}
#endif

package br.com.asfdev.chainway;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rscja.deviceapi.entity.UHFTAGInfo;
import com.rscja.deviceapi.interfaces.ConnectionStatus;

import java.util.ArrayList;

import br.com.asfdev.chainway.services.RFIDService;
import br.com.asfdev.chainway.utils.PermissionsChecker;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = getClass().getName();

    private PermissionsChecker permissionsChecker;
    private BluetoothAdapter bluetoothAdapter;

    private RFIDService rfidService;

    ListView listView;
    ArrayAdapter<String> adapter;
    ArrayList<String> arrayList = new ArrayList<>();

    Button button;
    Button button2;
    Button button3;
    Button button4;
    EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", arrayList.get(position));
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), "EPC copiado", Toast.LENGTH_SHORT).show();
            }
        });

        button = findViewById(R.id.button);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        button4 = findViewById(R.id.button4);

        editText = findViewById(R.id.epcText);

        button.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
        button4.setOnClickListener(this);

        permissionsChecker = new PermissionsChecker(this);
        permissionsChecker.call();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 1);
        }

        rfidService = new RFIDService(this,
                (connectionStatus, o) -> {
                    if (connectionStatus == ConnectionStatus.DISCONNECTED) {
                        disconnect();
                    }
                }
                , () -> {
            if (rfidService == null) return;
            if (editText.getText().toString().isEmpty()) {
                Toast.makeText(this, "Informe um EPC válido", Toast.LENGTH_SHORT).show();
                return;
            }
            autoLocationTag();
        }, (s, i) -> {
            if (i != -1) {
                runOnUiThread(() -> {
                    arrayList.add(editText.getText().toString() + " - Location: " + i);
                    adapter.notifyDataSetChanged();
                    listView.setSelection(adapter.getCount() - 1);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {

        if (rfidService != null) {
            rfidService.dispose();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // CONECTAR DISPOSITIVO
            case R.id.button:
                connect("DB:BD:A4:6B:9E:F5");
                break;
            // INICIAR LEITURA
            case R.id.button2:
                readTags();
                break;
            // LOCALIZAR EPC SINGLE
            case R.id.button3:
                singleLocationTag();
                break;
            // LOCALIZAR EPC AUTO
            case R.id.button4:
                autoLocationTag();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        arrayList.clear();
        adapter.notifyDataSetChanged();
    }

    public void connect(String address) {
        if (rfidService.isConnected()) {
            disconnect();
            return;
        }
        new Thread() {
            @Override
            public void run() {
                boolean res = rfidService.init(address);
                Log.d(TAG, "Connected to " + address + ": " + res);
                if (res) {
                    rfidService.registerKeyCallback();
                    runOnUiThread(() -> button.setText("Desconectar dispositivo"));
                }
            }
        }.start();
    }

    public void disconnect() {
        rfidService.dispose();
        button.setText("Conectar dispositivo");
        button2.setText("Iniciar leitura");
        button4.setText("Localizar EPC (Auto)");
    }

    public void readTags() {
        if (rfidService == null) return;

        if (!rfidService.isReading()) {
            if (rfidService.isConnected()) {
                button3.setEnabled(false);
                button4.setEnabled(false);
                button2.setText("Parar leitura");
                arrayList.clear();
                adapter.notifyDataSetChanged();
                rfidService.startInventory(tags -> {
                    for (UHFTAGInfo tag : tags) {
                        if (!arrayList.contains(tag.getEPC())) {
                            runOnUiThread(() -> {
                                arrayList.add(tag.getEPC());
                                adapter.notifyDataSetChanged();
                                listView.setSelection(adapter.getCount() - 1);
                            });
                        }
                    }
                });
            } else {
                Toast.makeText(getApplicationContext(), "O dispositivo não está conectado", Toast.LENGTH_SHORT).show();
            }
        } else {
            rfidService.stopInventory();
            button3.setEnabled(true);
            button4.setEnabled(true);
            button2.setText("Iniciar leitura");
        }
    }

    public void singleLocationTag() {
        if (editText.getText().toString().length() != 24) {
            Toast.makeText(getApplicationContext(), "O EPC não possui 24 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rfidService != null && rfidService.isConnected()) {
            int location = rfidService.singleLocation(editText.getText().toString());
            arrayList.add(editText.getText().toString() + " - Location: " + location);
            adapter.notifyDataSetChanged();
            listView.setSelection(adapter.getCount() - 1);
        } else {
            Toast.makeText(getApplicationContext(), "O dispositivo não está conectado", Toast.LENGTH_SHORT).show();
        }
    }

    public void autoLocationTag() {
        if (editText.getText().toString().length() != 24) {
            Toast.makeText(getApplicationContext(), "O EPC não possui 24 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rfidService != null && rfidService.isConnected()) {
            if (rfidService.isAutoLocation()) {
                rfidService.stopAutoLocation();
                button4.setText("Localizar EPC (Auto)");
                button2.setEnabled(true);
                button3.setEnabled(true);
            } else {
                arrayList.clear();
                adapter.notifyDataSetChanged();
                rfidService.startAutoLocation(editText.getText().toString());
                button4.setText("Parar localizador");
                button2.setEnabled(false);
                button3.setEnabled(false);
            }
        } else {
            Toast.makeText(getApplicationContext(), "O dispositivo não está conectado", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth habilitado", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Falha ao habilitar bluetooth", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }
}
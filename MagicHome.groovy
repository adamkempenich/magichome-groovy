require 'sinatra'
require 'openssl'
require 'csv'
require 'json'

require_relative 'config_provider'
config_provider = MagicHomeGateway::CachingConfigProvider.new(MagicHomeGateway::ConfigProvider.new)

before do

  timestamp = request.env['HTTP_X_SIGNATURE_TIMESTAMP']
  payload   = request.env['HTTP_X_SIGNATURE_PAYLOAD']
  signature = request.env['HTTP_X_SIGNATURE']

  halt 403 if payload.nil? or timestamp.nil? or signature.nil?

  digest = OpenSSL::Digest.new('sha1')
  data = (payload + timestamp)
  hmac = OpenSSL::HMAC.hexdigest(digest, config_provider.hmac_key, data)

  halt 403 unless hmac == signature
  halt 412 unless ((Time.now.to_i - 20) <= timestamp.to_i)
end


post '/leds' do

  # Convert the parameters into something usable
  params = request.env["rack.input"].read
  params = JSON.parse params.gsub('=>', ':')
  puts params.inspect

  # Initiate constants, if not already initiated
  WARMWHITEHUE ||= 16.66666666666666
  WARMWHITESATURATION ||= 27.45097875595093
  WARMWHITEHUE2 ||= 16

  # Convert the WW and CW levels to the device's specs
  ww = params['WWLevel'].to_i*2.55  if params['WWLevel'] != nil
  cw = params['CWLevel'].to_i*2.55 if params['CWLevel'] != nil

  # Prepare the RGB values to send to the device
  sendPreset = true if params.include?('preset')

  params.include?('red') && params.include?('green') && params.include?('blue') ? (useRGB = true) : (useRGB = false)

  if !useRGB && params.include?('hue') && params.include?('saturation') && params.include?('level')
    h = params['hue'].to_f*3.6
    s = params['saturation'].to_f
    l = params['level'].to_f
    l > 254 ? (l = 254) : ()
    l < 0 ? (l = 0) : ()

    r,g,b = hsvToRgb(h,s,l)
    params['red'] = r
    params['green'] = g
    params['blue'] = b
  elsif useRGB
    r = params['red'].to_i
    g = params['green'].to_i
    b = params['blue'].to_i
    useRGB = true
  end
  
  # Universally set which devices we'll call  
  params['bulb_ips'] != 'null' && params['bulb_ips'] != nil ? (use_bulbs = true) : (use_bulbs = false)
  params['rgb_ww_ips'] != 'null' && params['rgb_ww_ips'] != nil ? (use_rgb_ww = true) : (use_rgb_ww = false)
  params['rgb_ww_cw_ips'] != 'null' && params['rgb_ww_cw_ips'] != nil ? (use_rgb_ww_cw = true) : (use_rgb_ww_cw = false)
  params['legacy_bulb_ips'] != 'null' && params['legacy_bulb_ips'] != nil ? (use_legacy_bulbs = true) : (use_legacy_bulbs = false)

  # Set the dynamic variable names
  if use_bulbs
    bulb_names = ""
    params['bulb_ips'].delete(' ').parse_csv.each{|x| 
      bulb_names += params['bulb_ips'].delete('.').delete(',').delete(' ')
    }
  elsif use_rgb_ww
    rgb_ww_names = ""
    params['rgb_ww_ips'].delete(' ').parse_csv.each{|x| 
      rgb_ww_names += params['rgb_ww_ips'].delete('.').delete(',').delete(' ')
    }
  elsif use_rgb_ww_cw
    rgb_ww_cw_names = ""
    params['rgb_ww_cw_ips'].delete(' ').parse_csv.each{|x| 
      rgb_ww_cw_names += params['rgb_ww_cw_ips'].delete('.').delete(',').delete(' ')
    }
  elsif use_legacy_bulbs
    legacy_bulb_names = ""
    params['legacy_bulb_ips'].delete(' ').parse_csv.each{|x| 
      legacy_bulb_names += params['legacy_bulb_ips'].delete('.').delete(',').delete(' ')
    }
  end

  # Individual API calls for each type of device, if they exist
  use_bulbs && !instance_variable_defined?("@a#{bulb_names}") ? (instance_variable_set("@a#{bulb_names}", LEDENET::Api.new( params['bulb_ips'].delete(' ').parse_csv))) : ()
  use_rgb_ww && !instance_variable_defined?("@a#{rgb_ww_names}") ? (instance_variable_set("@a#{rgb_ww_names}", LEDENET::Api.new( params['rgb_ww_ips'].delete(' ').parse_csv))) : ()
  use_rgb_ww_cw && !instance_variable_defined?("@a#{rgb_ww_cw_names}") ? (instance_variable_set("@a#{rgb_ww_cw_names}", LEDENET::Api.new( params['rgb_ww_cw_ips'].delete(' ').parse_csv))) : ()
  use_legacy_bulbs && !instance_variable_defined?("@a#{legacy_bulb_names}") ? (instance_variable_set("@a#{legacy_bulb_names}", LEDENET::Api.new( params['legacy_bulb_ips'].delete(' ').parse_csv))) : ()

  # Turn all of the called devices on or off
  if params['status'] == 'on' || params['switch'] == 'on'
    puts "Turning devices on..."
    use_bulbs ? (instance_variable_get("@a#{bulb_names}").on) : ()
    use_rgb_ww ? (instance_variable_get("@a#{rgb_ww_names}").on) : ()
    use_rgb_ww_cw ? (instance_variable_get("@a#{rgb_ww_cw_names}").on) : ()
    use_legacy_bulbs ? (instance_variable_get("@a#{legacy_bulb_names}").legacy_bulb_on) : ()

  elsif params['status'] == 'off' || params['switch'] == 'off'
    puts "Shutting devices off..."
    use_bulbs ? (instance_variable_get("@a#{bulb_names}").off) : ()
    use_rgb_ww ? (instance_variable_get("@a#{rgb_ww_names}").off) : ()
    use_rgb_ww_cw ? (instance_variable_get("@a#{rgb_ww_cw_names}").off) : ()
    use_legacy_bulbs ? (instance_variable_get("@a#{legacy_bulb_names}").legacy_bulb_off) : ()

  end

  # Send a preset function to a device
  if sendPreset
    presets = { "1"=> 0x25, "2" => 0x26, "3" => 0x27, "4" => 0x28, "5" => 0x29, "6" => 0x30, "7" => 0x31, "8" => 0x32, "9" => 0x33, "10" => 0x34, "11" => 0x35, "12" => 0x36, "13" => 0x37, "14" => 0x38, "15" => 0x39, "16" => 0x40, "17" => 0x41, "18" => 0x42, "19" => 0x43, "20" => 0x44, "21" => 0x45 }
    speed = (100 - params['presetSpeed'].to_i)*2.55

    puts "Sending preset to devices..."
    use_bulbs ? (instance_variable_get("@a#{bulb_names}").send_default_function( presets[params['preset']], speed)) : ()
    use_rgb_ww ? (instance_variable_get("@a#{rgb_ww_names}").send_default_function( presets[params['preset']], speed)) : ()
    use_rgb_ww_cw ? (instance_variable_get("@a#{rgb_ww_cw_names}").send_default_function( presets[params['preset']], speed)) : ()
    use_legacy_bulbs ? (instance_variable_get("@a#{legacy_bulb_names}").send_legacy_default_function(presets[params['preset']], speed)) : ()
  end

  # Update all devices using RGB values
  if useRGB
    if params['hue'].to_f == WARMWHITEHUE || params['hue'].to_f == WARMWHITEHUE2
      l = params['level'].to_i*2.55
      l > 254 ? (l = 254) : ()
      l < 0 ? (l = 0) : ()
      
      use_bulbs ? (puts "Updating bulbs' white value") : ()
      use_bulbs ? (instance_variable_get("@a#{bulb_names}").update_bulb_white(l)) : ()
      use_legacy_bulbs ? (puts "Updating legacy bulbs' white value") : ()
      use_legacy_bulbs ? (instance_variable_get("@a#{legacy_bulb_names}").update_legacy_bulb_white(l)) : ()
    else
      use_bulbs ? (puts "Updating bulbs' color") : ()
      use_bulbs ? (instance_variable_get("@a#{bulb_names}").update_bulb_color(r,g,b)) : ()
      use_legacy_bulbs ? (puts "Updating legacy bulbs' colors") : ()
      use_legacy_bulbs ? (instance_variable_get("@a#{legacy_bulb_names}").update_legacy_bulb_color(r,g,b)) : ()
    end
    use_rgb_ww ? (puts "Updating RGB WW devices' colors") : ()
    use_rgb_ww ? (instance_variable_get("@a#{rgb_ww_names}").update_ww(r,g,b,ww)) : ()
    use_rgb_ww_cw ? (puts "Updating RGB WW CW devices' colors") : ()
    use_rgb_ww_cw ? (instance_variable_get("@a#{rgb_ww_cw_names}").update_ww_cw(r,g,b,ww,cw)) : ()
  end

  # if params.include?('refresh')
  #   if bulb_api != nil
  #   r,g,b,w,powerState = bulb_api.current_status
  #   h,s,l = rgbToHsv(r,g,b)
  #   r,g,b = hsvToRgb(WARMWHITEHUE, WARMWHITESATURATION, (w/2.55)) if [r,g,b] == [0,0,0] && w > 0
  #   headers \
  #       "powerState" => powerState,
  #       "level" => l.to_s,
  #       "hex" =>  "#" + to_hex(r) + to_hex(g) + to_hex(b),
  #   end

  #   if rgb_api != nil
  #   r,g,b,w,powerState = rgb_api.current_status
  #   h,s,l = rgbToHsv(r,g,b)
  #     headers \
  #       "powerState" => powerState,
  #       "level" => l.to_s,
  #       "hex" =>  "#" + to_hex(r) + to_hex(g) + to_hex(b),
  #   end
  #   if rgb_ww_api != nil
  #   r,g,b,w,powerState = rgb_ww_api.current_status
  #   h,s,l = rgbToHsv(r,g,b)
  #     headers \
  #       "powerState" => powerState,
  #       "level" => l.to_s,
  #       "hex" =>  "#" + to_hex(r) + to_hex(g) + to_hex(b),
  #       "WWLevel" => (w/2.55).to_s
  #   end

  #   if rgb_ww_cw_api != nil
  #   r,g,b,w,cw,powerState = rgb_ww_cw_api.current_status
  #   h,s,l = rgbToHsv(r,g,b)
  #     headers \
  #       "powerState" => powerState,
  #       "level" => l.to_s,
  #       "hex" =>  "#" + to_hex(r) + to_hex(g) + to_hex(b),
  #       "CWLevel" => (w/2.55).to_s
  #   end
  # end
  status 200
  body '{"success": true}'
end

# def rgbToHsv(r, g, b)
#   # Takes an RGB value (0-255) and returns HSV in 0-360, 0-100, 0-100
#   r /= 255.0
#   g /= 255.0
#   b /= 255.0

#   max = [r, g, b].max.to_f
#   min = [r, g, b].min.to_f
#   delta = (max - min).to_f
#   v = (max * 100.0).to_f

#   max != 0.0 ? s = delta / max * 100.0 : s=0
  
#   if (s == 0.0) 
#     h = 0.0
#   else
#       if (r == max)
#         h = ((g - b) / delta).to_f
#       elsif (g == max)
#         h = (2 + (b - r) / delta).to_f
#       elsif (b == max)
#         h = (4 + (r - g) / delta).to_f
#     end
#     h *= 60.0
#     h += 360 if (h < 0)
#   end
#   return h,s,v
# end
# def hsvToRgb(h,s,v)
#   h /= 360.0
#   s /= 100.0
#   v /= 100.0

#   if s == 0.0
#      r = v * 255
#      g = v * 255
#      b = v * 255
#   else
#     h = (h * 6).to_f
#     h = 0 if h == 6
#     i = h.floor
#     var_1 = (v * ( 1.0 - s )).to_f
#     var_2 = (v * ( 1.0 - s * ( h - i ) )).to_f
#     var_3 = (v * ( 1.0 - s * ( 1.0 - ( h - i )))).to_f
#   end

#   if i == 0 
#     r = v
#     g = var_3
#     b = var_1
#   elsif i == 1
#     r = var_2
#     g = v
#     b = var_1
#   elsif i == 2
#     r = var_1
#     g = v
#     b = var_3
#   elsif i == 3
#     r = var_1
#     g = var_2
#     b = v
#   elsif i == 4
#     r = var_3
#     g = var_1
#     b = v
#   else
#     r = v
#     g = var_1
#     b = var_2
#   end

#     if r==nil
#       r=0
#     end
#     if g==nil
#       g=0
#     end
#     if b==nil
#       b=0
#     end

#     r *= 255
#     g *= 255
#     b *= 255

#   return r.to_i, g.to_i, b.to_i
# end

def to_hex(number)
  number.to_s(16).upcase.rjust(2, '0')
end

module LEDENET
  class Api
    API_PORT ||= 5577
    
    $dynamic_variables ||= Hash.new
    $reset_table ||= Hash.new

    DEFAULT_OPTIONS ||= {
        reuse_connection: true,
        max_retries: 2
    }


    def initialize(device_address, options = {})
      @device_address = device_address
      @options = DEFAULT_OPTIONS.merge(options)
    end

    def on
      send_bytes_action(0x71, 0x23, 0x0F, 0xA3)
      true
    end

    def legacy_bulb_on
      send_bytes_action(0xCC, 0x23, 0x33)
      true
    end

    def off
      send_bytes_action(0x71, 0x24 ,0x0F, 0xA4)
      true
    end

    def legacy_bulb_off
      send_bytes_action(0xCC, 0x24, 0x33)
      true
    end

    def update_ww(r, g, b, ww) # Update a WW wireless device
      msg = [0x31, r, g, b, ww, 0x00, 0x0f]

      send_bytes_action(*msg, calc_checksum(msg))
      true
    end

    def update_ww_cw(r, g, b, ww, cw)
      # Update Color
      msg = [0x31, r, g, b, 0x00, 0x00, 0xf0, 0x0f]
      send_bytes_action(*msg, calc_checksum(msg))

      # Update WW/CW
      msg = [0x31, 0x00, 0x00, 0x00, ww, cw, 0x0f, 0x0f]
      send_bytes_action(*msg, calc_checksum(msg))

      
      true
    end

    def update_rgb(r, g, b)
      msg = [0x31, r, g, b, 0x00, 0x0f]
      send_bytes_action(*msg, calc_checksum(msg))
      true
    end

    def update_bulb_color(r, g, b) # Update a Bulb wireless device's color
      msg = [0x31, r, g, b, 0x00, 0xf0, 0x0f]

      send_bytes_action(*msg, calc_checksum(msg))
      true
    end

    def update_legacy_bulb_color(r,g,b)
      msg = [0x56, r, g, b, 0x00, 0xf0, 0xaa]

      send_bytes_action(*msg)
      true
    end

    def update_bulb_white(w) # Update a Bulb wireless device's WW level
        msg = [0x31, 0x00, 0x00, 0x00, w, 0x0f, 0x0f]
        send_bytes_action(*msg, calc_checksum(msg))
        true
    end

    def update_legacy_bulb_white(w)
      msg = [0x56, 0x00, 0x00, 0x00, w, 0x0f, 0xaa]
        send_bytes_action(*msg)
        true
    end

    def send_default_function(number, speed)
      # Function number is 0x25 to 0x38
      # Mode, Function Number, WW, Return?, Checksum
      # 0x61 0x25 0x01 0x0f 0x96
      # 25... Seven color crossfade, 26 red gradual change, 27 green gradual change, 28 blue gradual change, 29 yellow gradul change, 30 cyan gradual change, 31 purple gradual change, 32 white gradual change
      # 33 red green crossfade, 34 red blue crossfade, 35 green blue crossfade, 36 seven color strobe flash, 37 red strobe flash, 38green strobe flash, 39 blue strobe flash, 40 yellow strobe flash,
      # 41 cyan strobe flash, 42 purple strobe flash, 43 white strobe flash, 44 white strobe flash, 45 seven color jumping change


      # Speed is an inverted percent from 0x00 to 0x64
      data = [0x61, number, speed, 0x0F]
      send_bytes_action(*data, calc_checksum(data))
    end 
    
    def send_legacy_default_function(number, speed)
      data = [0xBB, number, speed, 0x44]
      send_bytes_action(*data)
    end

    def send_custom_function(colors_array, speed, type )
      #0x51, [16 color bytes],  Marker,   speed,  type (3a, 3b, 3c),  0xff, 0x0f, checksum
      #0x51,  [16 color bytes],  0x00,     0x0b,   0x3c,               0xff, 0x0f, 0x59

      data = [0x51, color_array, 0x00, speed, type, 0xff, 0x0f]

      send_bytes_action(data, calc_checksum(data))
    end

    def ping # Returns R, G, B, W, Power State, and Device Type
      return status
    end

    def current_status # Returns R, G, B, W, Power State, and Device Type
      current_packet = status
      return unpack_to_int(current_packet[6]), unpack_to_int(current_packet[7]), unpack_to_int(current_packet[8]), unpack_to_int(current_packet[9]), on?, device_type?
    end
    
    def reconnect!
      create_socket(current_address)
    end

    def getInfo
      msg = Integer(status.each.unpack('C').to_s.delete('[]'))
    end

    private
      def calc_checksum(bytes)
        bytes.reduce(:+) %0x100
      end

      def unpack_to_int(packed_byte)
         # Unpacks a byte and returns it as an int
        packed_byte.unpack('C').to_s.delete('[]').to_i
      end

      def status
        socket_action do
          msg = [0x81, 0x8A, 0x8B, calc_checksum([0x81, 0x8A, 0x8B])]
          send_bytes(*msg)
          flush_response(14)
        end
      end

      def flush_response(msg_length)
        @socket.recv(msg_length, Socket::MSG_WAITALL)
      end


      def send_bytes(*b)
        puts "Sending #{b} to #{@socket}"
        @socket.write(b.pack('c*'))
      end

      def send_bytes_action(*b)
        socket_action { send_bytes(*b) }
      end

      def create_socket(ip)

        # Prepare the passed data for use
        ip_name = ip.delete('.')
        # puts "Dynamic variables are: #{$dynamic_variables}"

        if !$reset_table["@a#{ip_name}"]
            $reset_table["@a#{ip_name}"] = Time.now
        end

        elapsed_seconds = ((Time.now - $reset_table["@a#{ip_name}"])).to_i  

        # If the variable doesn't exist, create it
        if !$dynamic_variables["@a#{ip_name}"] or $dynamic_variables["@a#{ip_name}"].closed? or elapsed_seconds >= 250
          # puts "Setting a new variable, @a#{ip_name}"
          $dynamic_variables["@a#{ip_name}"] = TCPSocket.new(ip, API_PORT)
        end

        # Return our light object
        $reset_table["@a#{ip_name}"] = Time.now
        @socket = $dynamic_variables["@a#{ip_name}"]
      end

      def socket_action
        Array(@device_address).each{ |current_address|
          tries = 0
          begin
            create_socket(current_address)
            yield
          rescue Errno::EPIPE, IOError => e
            tries += 1

            if tries <= @options[:max_retries]
              puts "Establishing reconnection"
              reconnect!
              retry
            else
              raise e
            end
          ensure
            puts "Closing connection" unless @options[:reuse_connection]
            @socket.close unless @options[:reuse_connection]
          end
        }
      end
    Thread.new do
      loop do
        sleep 50
        puts "Checking devices' connections"
        
          $dynamic_variables.each do |key, api|
            puts "Checking Device ... #{key}"

            elapsed_seconds = ((Time.now - $reset_table[key])).to_i  
            if elapsed_seconds >= 250
              puts "Resetting the timer... The key is #{key} and the api is #{api}"
              puts "We're connected to #{api.peeraddr}"
              $dynamic_variables[key] = TCPSocket.new(api.peeraddr[2], api.peeraddr[1])
              puts "We're connected to #{api.peeraddr}"
              $reset_table[key] = Time.now
              puts "Reset #{key} after #{elapsed_seconds}."
            end
          end
      end
    end
  end
end
